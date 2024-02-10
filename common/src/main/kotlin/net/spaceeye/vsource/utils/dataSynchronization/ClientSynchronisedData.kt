package net.spaceeye.vsource.utils.dataSynchronization

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.networking.Serializable
import net.spaceeye.vsource.networking.S2CConnection
import java.util.function.Supplier

abstract class ClientSynchronisedData<T: DataUnit>(id: String, getServerInstance: () -> ServerSynchronisedData<T>, val supplier: Supplier<T>) {
    val serverRequestChecksumResponseConnection = id idWithConn ::ServerDataResponseConnection
    val serverDataUpdateRequestResponseConnection = id idWithConn ::ServerDataUpdateRequestResponseConnection

    val dataRequestChecksumConnection = {getServerInstance().dataRequestChecksumConnection}
    val dataUpdateRequestConnection   = {getServerInstance().dataUpdateRequestConnection}

    //TODO change to synchronised map
    private val serverChecksums = mutableMapOf<Long, MutableMap<Int, ByteArray>>()
    private val clientChecksums = mutableMapOf<Long, MutableMap<Int, ByteArray>>()

    protected val cachedData = mutableMapOf<Long, MutableMap<Int, T>>()

    fun tryPoolDataUpdate(page: Long): MutableMap<Int, T>? {
        if (!serverChecksums.containsKey(page)) {
            requestChecksumsUpdate(page)
            return cachedData[page]
        }
        if (clientChecksums.containsKey(page)
            && (clientChecksums[page]!!.size == serverChecksums[page]!!.size)
            && clientChecksums[page]!!.all {
                           serverChecksums[page]!!.containsKey(it.key)
                        && serverChecksums[page]!![it.key].contentEquals(it.value)
                        }
            ) {
            return cachedData[page]
        }
        requestUpdateData(page)
        return cachedData[page]
    }

    fun requestChecksumsUpdate(page: Long) {
        dataRequestChecksumConnection().sendToServer(ClientDataRequestPacket(page))
    }

    fun requestUpdateData(page: Long) {
        val serverPage = serverChecksums[page]!!
        val clientPage = clientChecksums[page]!!

        val serverIds = serverPage.keys
        var clientIds = clientPage.keys

        clientIds.filter { !clientIds.containsAll(serverIds) }.forEach {clientPage.remove(it)}
        clientIds = clientPage.keys

        val toUpdate = serverPage.filter { (k, v) -> !clientPage[k]!!.contentEquals(v) }.map { it.key }.toMutableList()

        dataUpdateRequestConnection().sendToServer(ClientDataUpdateRequestPacket(page, toUpdate))
    }

    class ServerDataResponseConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerDataRequestResponsePacket>(id, "server_data_request_response_packet") {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerDataRequestResponsePacket(buf)
            if (!packet.pageExists) {
                clientInstance.serverChecksums.remove(packet.page)
                clientInstance.clientChecksums.remove(packet.page)
                clientInstance.cachedData     .remove(packet.page)
                return
            }

            clientInstance.serverChecksums[packet.page] = packet.checksums.toMap().toMutableMap()
        }
    }

    class ServerDataUpdateRequestResponseConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerDataUpdateRequestResponsePacket<T>>(id, "server_data_update_request_response_packet") {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerDataUpdateRequestResponsePacket(buf, clientInstance.supplier)
            if (!packet.pageExists) {
                clientInstance.serverChecksums.remove(packet.page)
                clientInstance.clientChecksums.remove(packet.page)
                clientInstance.cachedData     .remove(packet.page)
                return
            }
            val page = clientInstance.cachedData[packet.page] ?: return
            packet.newData.forEach { (idx, item) -> page[idx] = item}
            packet.nullData.forEach { page.remove(it) }
        }
    }

    infix fun <TT: Serializable> String.idWithConn(constructor: (String, ClientSynchronisedData<T>) -> S2CConnection<TT>): S2CConnection<TT> {
        val instance = constructor(this, this@ClientSynchronisedData)
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        return instance
    }
}