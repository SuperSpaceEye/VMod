package net.spaceeye.vsource.utils.dataSynchronization

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.networking.Serializable


abstract class ServerSynchronisedData<T: DataUnit>(id: String, getClientInstance: () -> ClientSynchronisedData<T>) {
    val data = mutableMapOf<Long, MutableMap<Int, T>>()

    val serverRequestChecksumResponseConnection   = {getClientInstance().serverRequestChecksumResponseConnection}
    val serverDataUpdateRequestResponseConnection = {getClientInstance().serverDataUpdateRequestResponseConnection}

    // CONNECTIONS FOR CLIENT, HANDLERS FOR SERVER
    val dataRequestChecksumConnection = id idWithConn ::ClientDataRequestConnection
    val dataUpdateRequestConnection   = id idWithConn ::ClientDataUpdateRequestConnection

    class ClientDataRequestConnection<T: DataUnit>(id: String, val serverInstance: ServerSynchronisedData<T>): C2SConnection<ClientDataRequestPacket>(id, "client_data_request_connection") {
        override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ClientDataRequestPacket(buf)
            if (!serverInstance.data.containsKey(packet.page)) {
                serverInstance.serverRequestChecksumResponseConnection().sendToClient(
                    context.player as ServerPlayer,
                    ServerDataRequestResponsePacket(packet.page, false)
                )
                return
            }
            val page = serverInstance.data[packet.page]!!
            val checksums = page.map {(k, v) -> Pair(k, v.hash())}
            //TODO rework this so that it could send it in pieces
            serverInstance.serverRequestChecksumResponseConnection().sendToClient(
                context.player as ServerPlayer,
                ServerDataRequestResponsePacket(packet.page, true, checksums.toMutableList())
            )
        }
    }

    class ClientDataUpdateRequestConnection<T : DataUnit>(id: String, val serverInstance: ServerSynchronisedData<T>): C2SConnection<ClientDataUpdateRequestPacket>(id, "client_data_update_request_packet") {
        override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ClientDataUpdateRequestPacket(buf)
            val page = serverInstance.data[packet.page]
            if (page == null) {
                serverInstance.serverDataUpdateRequestResponseConnection().sendToClient(
                    context.player as ServerPlayer,
                    ServerDataUpdateRequestResponsePacket(false, packet.page, null)
                )
                return
            }
            val dataToSend = page.filterKeys { packet.indicesToUpdate.contains(it) }.toList().toMutableList()
            val nullData = packet.indicesToUpdate.filter { !page.keys.contains(it) }.toMutableList()

            serverInstance.serverDataUpdateRequestResponseConnection().sendToClient(
                context.player as ServerPlayer,
                ServerDataUpdateRequestResponsePacket(true, packet.page, dataToSend, nullData, null)
            )
        }
    }

    infix fun <TT: Serializable> String.idWithConn(constructor: (String, ServerSynchronisedData<T>) -> C2SConnection<TT>): C2SConnection<TT> {
        val instance = constructor(this, this@ServerSynchronisedData)
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        return instance
    }
}