package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.getNow_ms
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.min

object DataStreamReceiverDataHolder: ServerClosable() {
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

object DataStreamTransmitterDataHolder: ServerClosable() {
    val requestsHolder = ConcurrentHashMap<UUID, RequestItem>()

    init {
        thread {
            while (true) {
                synchronized(requestsHolder) {
                    val now = getNow_ms()

                    val toRemove = mutableListOf<UUID>()
                    for ((k, item) in requestsHolder) {
                        if (now - item.lastUpdated > 100 * 1000) {
                            toRemove.add(k)
                            continue
                        }
                    }
                    toRemove.forEach { requestsHolder.remove(it) }
                }
                Thread.sleep(5 * 1000)
            }
        }
    }

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

class S2CDataStream<
        TRequest: Serializable,
        TData: Serializable>(
    streamName: String,
    partByteAmount: Int = 524288,
    requestPacketConstructor: () -> TRequest,
    requestProcessor: (req: TRequest) -> TData?,
    dataPacketConstructor: () -> TData,
    dataTransmitted: (uuid: UUID, data: TData?) -> Unit,
    ): NetworkingRegisteringFunctions {

    private fun sendPart(player: Player, uuid: UUID, part: Int) {
        val req = DataStreamTransmitterDataHolder.requestsHolder[uuid] ?: return

        WLOG("SENDING PART $part")

        val partData = mutableListOf<Byte>()
        req.data.forEachByte((part - 1) * req.partByteAmount, min(req.data.array().size - (part - 1) * req.partByteAmount, req.partByteAmount)) { partData.add(it) }
        req.lastUpdated = getNow_ms()
        s2cSendPart.sendToClient(player as ServerPlayer, DataPartPkt(part, req.numParts, uuid, partData.toByteArray()))
    }

    val c2sRequestData = "request_data" idWithConnc {
        object : C2SConnection<TRequest>(it, streamName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {

                val pkt = requestPacketConstructor()
                pkt.deserialize(buf)
                val data = requestProcessor(pkt)
                if (data == null) {
                    s2cRequestFailure.sendToClient(context.player as ServerPlayer, RequestFailurePkt())
                    return
                }
                val dataBuf = data.serialize()
                val length = dataBuf.array().size / partByteAmount + 1
                val uuid = DataStreamTransmitterDataHolder.addRequest(DataStreamTransmitterDataHolder.RequestItem(dataBuf, partByteAmount, length))
                sendPart(context.player, uuid, 1)
            }
        }
    }
    private val s2cRequestFailure = "request_failure" idWithConns {
        object : S2CConnection<RequestFailurePkt>(it, streamName) {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                ELOG("REQUEST FAILED")
            }
        }
    }

    val s2cSendPart = "send_part" idWithConns {
        object : S2CConnection<DataPartPkt>(it, streamName) {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = DataPartPkt(buf)
                val data = DataStreamReceiverDataHolder.updateReceived(pkt.requestUUID, pkt.data)

                if (pkt.part == pkt.maxParts) {
                    val dataPkt = dataPacketConstructor()
                    dataPkt.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(data.toByteArray())))
                    DataStreamReceiverDataHolder.receivedData.remove(pkt.requestUUID)
                    dataTransmitted(pkt.requestUUID, dataPkt)
                    return
                }

                c2sRequestPart.sendToServer(RequestPart(pkt.part+1, pkt.requestUUID))
            }
        }
    }
    val c2sRequestPart = "request_part" idWithConnc {
        object : C2SConnection<RequestPart>(it, streamName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = RequestPart(buf)
                val item = DataStreamTransmitterDataHolder.requestsHolder[pkt.requestUUID]
                if (item == null) {
                    s2cRequestFailure.sendToClient(context.player as ServerPlayer, RequestFailurePkt())
                    return
                }
                sendPart(context.player, pkt.requestUUID, pkt.partNum)
            }
        }
    }

    class RequestFailurePkt(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    class DataPartPkt(): Serializable {
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

    class RequestPart(): Serializable {
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