package net.spaceeye.vsource.networking.dataSynchronization

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.networking.Serializable
import net.spaceeye.vsource.utils.ServerClosable


abstract class ServerSynchronisedData<T: DataUnit>(val id: String, getClientInstance: () -> ClientSynchronisedData<T>): ServerClosable() {
    //TODO i think this can crash and i need to use ConcurrentHashMap
    val data = mutableMapOf<Long, MutableMap<Int, T>>()

    val serverRequestChecksumResponseConnection   = {getClientInstance().serverRequestChecksumResponseConnection}
    val serverDataUpdateRequestResponseConnection = {getClientInstance().serverDataUpdateRequestResponseConnection}
    val serverChecksumsUpdatedConnection          = {getClientInstance().serverChecksumsUpdatedConnection}

    // CONNECTIONS FOR CLIENT, HANDLERS FOR SERVER
    val dataRequestChecksumConnection = id idWithConn ::ClientDataRequestConnection
    val dataUpdateRequestConnection   = id idWithConn ::ClientDataUpdateRequestConnection

    override fun close() {
        data.clear()
    }

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
            //TODO rework this so that it could send data in pieces
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
                    ServerDataUpdateRequestResponsePacket(false, packet.page)
                )
                return
            }
            val dataToSend = page.filterKeys { packet.indicesToUpdate.contains(it) }.toList().toMutableList()
            val nullData = packet.indicesToUpdate.filter { !page.keys.contains(it) }.toMutableList()

            serverInstance.serverDataUpdateRequestResponseConnection().sendToClient(
                context.player as ServerPlayer,
                ServerDataUpdateRequestResponsePacket(true, packet.page, dataToSend, nullData)
            )
        }
    }

    infix fun <TT: Serializable> String.idWithConn(constructor: (String, ServerSynchronisedData<T>) -> C2SConnection<TT>): C2SConnection<TT> {
        val instance = constructor(this, this@ServerSynchronisedData)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}