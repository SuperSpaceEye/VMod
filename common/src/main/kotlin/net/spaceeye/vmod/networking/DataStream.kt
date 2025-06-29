package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.PacketContext
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.getNow_ms
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

abstract class DataStream<
        TRequest: Serializable,
        TData: Serializable>(
    streamName: String,
    transmitterSide: NetworkManager.Side,
    currentSide: NetworkManager.Side,
    modId: String,
    unified: Boolean = false,
    receiverWrapper:    (fn: () -> Unit) -> Unit = {it()},
    transmitterWrapper: (fn: () -> Unit) -> Unit = {it()},
    ) {
    constructor(
        streamName: String,
        transmitterSide: NetworkManager.Side,
        modId: String,
        receiverWrapper:    (fn: () -> Unit) -> Unit = {it()},
        transmitterWrapper: (fn: () -> Unit) -> Unit = {it()},
    ): this(streamName, transmitterSide, NetworkManager.Side.S2C, modId, true, receiverWrapper, transmitterWrapper)

    open val partByteAmount: Int = 30000

    /**
     * Has to create but not deserialize
     */
    abstract fun requestPacketConstructor(buf: FriendlyByteBuf): TRequest
    abstract fun dataPacketConstructor(): TData

    // if returns null, then will do nothing
    // if not null, will either start transmitting or send transmission failed pkt
    abstract fun transmitterRequestProcessor(req: TRequest, ctx: PacketContext): Either<TData, RequestFailurePkt>?

    abstract fun receiverDataTransmitted(uuid: UUID, data: TData, ctx: PacketContext)
    abstract fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt)

    // for when you need to restrict access to some receivers
    open fun uuidHasAccess(uuid: UUID, ctx: PacketContext, req: TRequest): Boolean = true

    private val transmitterData = DataStreamTransmitterDataHolder()
    private val receiverData = DataStreamReceiverDataHolder()

    private inline fun verifyUUIDHasAccess(context: PacketContext, pkt: TRequest, fn: (pkt: TRequest) -> Unit) {
        val uuid = context.player?.uuid
        if (uuid != null && !uuidHasAccess(uuid, context, pkt)) {
            t2rRequestFailure.transmitData(RequestFailurePkt(), context)
            return
        }
        fn(pkt)
    }

    private fun sendPart(context: PacketContext, uuid: UUID, part: Int) {
        val req = transmitterData.requestsHolder[uuid] ?: return

        val partData = mutableListOf<Byte>()
        req.data.forEachByte((part - 1) * req.partByteAmount, min(req.data.array().size - (part - 1) * req.partByteAmount, req.partByteAmount)) { partData.add(it) }
        req.lastUpdated = getNow_ms()
        t2rSendPart.transmitData(DataPartPkt(part, req.numParts, uuid, partData.toByteArray()), context)
    }

    fun startSendingDataToReceiver(data: TData, context: PacketContext) {
        val dataBuf = data.serialize()
        val length = dataBuf.array().size / partByteAmount + 1
        val uuid = transmitterData.addRequest(DataStreamTransmitterDataHolder.RequestItem(dataBuf, partByteAmount, length))
        sendPart(context, uuid, 1)
    }

    private inline fun deserializeRequestPkt(buf: FriendlyByteBuf, fn: (TRequest) -> Unit) = fn(requestPacketConstructor(buf).also { it.deserialize(buf) })

    // is invoked on the receiver, handler is registered on the transmitter
    val r2tRequestData = registerTR("request_data", currentSide, unified) {
        object : TRConnection<TRequest>(transmitterSide.opposite()) {
            override val id = ResourceLocation(modId, "tr_${streamName}_$it")
            override fun handlerFn(buf: FriendlyByteBuf, context: PacketContext)
            = deserializeRequestPkt(buf) { pkt -> transmitterWrapper { verifyUUIDHasAccess(context, pkt) { pkt ->
                val data = transmitterRequestProcessor(pkt, context) ?: return@verifyUUIDHasAccess

                if (data is Either.Right){ t2rRequestFailure.transmitData(data.b, context); return@verifyUUIDHasAccess }

                startSendingDataToReceiver((data as Either.Left).a, context)
            } } }
        }
    }
    private val t2rRequestFailure = registerTR("request_failure", currentSide, unified) {
        object : TRConnection<RequestFailurePkt>(transmitterSide) {
            override val id = ResourceLocation(modId, "tr_${streamName}_$it")
            override fun handlerFn(buf: FriendlyByteBuf, context: PacketContext) = receiverWrapper {
                receiverDataTransmissionFailed(RequestFailurePkt(buf))
            }
        }
    }

    private val t2rSendPart = registerTR("send_part", currentSide, unified) {
        object : TRConnection<DataPartPkt>(transmitterSide) {
            override val id = ResourceLocation(modId, "tr_${streamName}_$it")
            override fun handlerFn(buf: FriendlyByteBuf, context: PacketContext) {
                val pkt = DataPartPkt(buf)
                val data = receiverData.updateReceived(pkt.requestUUID, pkt.data)

                if (pkt.part == pkt.maxParts) {
                    val dataPkt = dataPacketConstructor()
                    dataPkt.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(data.toByteArray())))
                    receiverData.receivedData.remove(pkt.requestUUID)
                    receiverWrapper { receiverDataTransmitted(pkt.requestUUID, dataPkt, context) }
                    return
                }

                r2tRequestPart.transmitData(RequestPart(pkt.part+1, pkt.requestUUID), context)
            }
        }
    }

    private val r2tRequestPart = registerTR("request_part", currentSide, unified) {
        object : TRConnection<RequestPart>(transmitterSide.opposite()) {
            override val id = ResourceLocation(modId, "tr_${streamName}_$it")
            override fun handlerFn(buf: FriendlyByteBuf, context: PacketContext) {
                val pkt = RequestPart(buf)
                val item = transmitterData.requestsHolder[pkt.requestUUID]
                if (item == null) {
                    t2rRequestFailure.transmitData(RequestFailurePkt(), context)
                    return
                }
                sendPart(context, pkt.requestUUID, pkt.partNum)
            }
        }
    }

    private class DataStreamReceiverDataHolder: ServerClosable() {
        val receivedData = ConcurrentHashMap<UUID, ReceiverItem>()

        fun updateReceived(uuid: UUID, data: ByteArray): MutableList<Byte> {
            val item = receivedData.getOrPut(uuid) { ReceiverItem(mutableListOf()) }
            item.data.addAll(data.toMutableList())
            return item.data
        }

        data class ReceiverItem(val data: MutableList<Byte>, var lastUpdated: Long = getNow_ms())

        override fun close() {
            receivedData.clear()
        }
    }

    private class DataStreamTransmitterDataHolder: ServerClosable() {
        val requestsHolder = ConcurrentHashMap<UUID, RequestItem>()

        fun addRequest(requestItem: RequestItem): UUID {
            val uuid = UUID.randomUUID()

            requestsHolder[uuid] = requestItem

            return uuid
        }

        override fun close() {
            requestsHolder.clear()
        }

        data class RequestItem(
            val data: FriendlyByteBuf,
            val partByteAmount: Int,
            val numParts: Int,
            var currentPart: Int = 0,
            var lastUpdated: Long = getNow_ms())
    }

    class RequestFailurePkt(): Serializable {
        var extraData = byteArrayOf()

        constructor(extraData: ByteArray): this() {this.extraData = extraData}
        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeByteArray(extraData)

            return buf
        }
        override fun deserialize(buf: FriendlyByteBuf) {
            extraData = buf.readByteArray()
        }
    }

    private class DataPartPkt(): Serializable {
        var part: Int = 0
        var maxParts: Int = 0
        lateinit var requestUUID: UUID
        lateinit var data: ByteArray

        constructor(part: Int, maxParts: Int, requestUUID: UUID, data: ByteArray): this() {
            this.part = part
            this.maxParts = maxParts
            this.requestUUID = requestUUID
            this.data = data
        }

        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeInt(part)
            buf.writeInt(maxParts)
            buf.writeUUID(requestUUID)
            buf.writeByteArray(data)

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            part = buf.readInt()
            maxParts = buf.readInt()
            requestUUID = buf.readUUID()
            data = buf.readByteArray()
        }
    }

    private class RequestPart(): Serializable {
        var partNum = 0
        lateinit var requestUUID: UUID

        constructor(parNum: Int, requestUUID: UUID): this() {
            this.partNum = parNum
            this.requestUUID = requestUUID
        }

        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeInt(partNum)
            buf.writeUUID(requestUUID)

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            partNum = buf.readInt()
            requestUUID = buf.readUUID()
        }
    }
}