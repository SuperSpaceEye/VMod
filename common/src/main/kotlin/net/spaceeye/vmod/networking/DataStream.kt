package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
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
    partByteAmount: Int = 1000000,
    ): NetworkingRegisteringFunctions {

    abstract fun requestPacketConstructor(): TRequest
    abstract fun dataPacketConstructor(): TData

    abstract fun transmitterRequestProcessor(req: TRequest): Either<TData, RequestFailurePkt>?

    abstract fun receiverDataTransmitted(uuid: UUID, data: TData?)
    abstract fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt)

    private val transmitterData = DataStreamTransmitterDataHolder()
    private val receiverData = DataStreamReceiverDataHolder()

    private fun sendPart(context: NetworkManager.PacketContext, uuid: UUID, part: Int) {
        val req = transmitterData.requestsHolder[uuid] ?: return

        val partData = mutableListOf<Byte>()
        req.data.forEachByte((part - 1) * req.partByteAmount, min(req.data.array().size - (part - 1) * req.partByteAmount, req.partByteAmount)) { partData.add(it) }
        req.lastUpdated = getNow_ms()
        t2rSendPart.transmitData(DataPartPkt(part, req.numParts, uuid, partData.toByteArray()), context)
    }

    // is invoked on the receiver, handler is registered on the transmitter
    val r2tRequestData = registerTR("request_data", currentSide) {
        object : TRConnection<TRequest>(it, streamName, transmitterSide.opposite()) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = requestPacketConstructor()
                pkt.deserialize(buf)
                val data = transmitterRequestProcessor(pkt)

                if (data == null) { t2rRequestFailure.transmitData(RequestFailurePkt(), context); return }
                if (data is Either.Right){ t2rRequestFailure.transmitData(data.b, context); return }

                val dataBuf = (data as Either.Left).a.serialize()
                val length = dataBuf.array().size / partByteAmount + 1
                val uuid = transmitterData.addRequest(DataStreamTransmitterDataHolder.RequestItem(dataBuf, partByteAmount, length))
                sendPart(context, uuid, 1)
            }
        }
    }
    private val t2rRequestFailure = registerTR("request_failure", currentSide) {
        object : TRConnection<RequestFailurePkt>(it, streamName, transmitterSide) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                receiverDataTransmissionFailed(RequestFailurePkt(buf))
            }
        }
    }

    private val t2rSendPart = registerTR("send_part", currentSide) {
        object : TRConnection<DataPartPkt>(it, streamName, transmitterSide) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = DataPartPkt(buf)
                val data = receiverData.updateReceived(pkt.requestUUID, pkt.data)

                if (pkt.part == pkt.maxParts) {
                    val dataPkt = dataPacketConstructor()
                    dataPkt.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(data.toByteArray())))
                    receiverData.receivedData.remove(pkt.requestUUID)
                    receiverDataTransmitted(pkt.requestUUID, dataPkt)
                    return
                }

                r2tRequestPart.transmitData(RequestPart(pkt.part+1, pkt.requestUUID), context)
            }
        }
    }

    private val r2tRequestPart = registerTR("request_part", currentSide) {
        object : TRConnection<RequestPart>(it, streamName, transmitterSide.opposite()) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
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