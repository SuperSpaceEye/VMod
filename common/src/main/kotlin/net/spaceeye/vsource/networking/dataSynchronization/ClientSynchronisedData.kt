package net.spaceeye.vsource.networking.dataSynchronization

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.networking.Serializable
import net.spaceeye.vsource.networking.S2CConnection
import net.spaceeye.vsource.utils.ClientClosable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

abstract class ClientSynchronisedData<T: DataUnit>(id: String, getServerInstance: () -> ServerSynchronisedData<T>) : ClientClosable() {
    val serverRequestChecksumResponseConnection = id idWithConn ::ServerDataResponseConnection
    val serverDataUpdateRequestResponseConnection = id idWithConn ::ServerDataUpdateRequestResponseConnection
    val serverChecksumsUpdatedConnection = id idWithConn ::ServerChecksumsUpdatedConnection

    val dataRequestChecksumConnection = {getServerInstance().dataRequestChecksumConnection}
    val dataUpdateRequestConnection   = {getServerInstance().dataUpdateRequestConnection}

    val serverChecksums = ConcurrentHashMap<Long, ConcurrentHashMap<Int, ByteArray>>()
    val clientChecksums = ConcurrentHashMap<Long, ConcurrentHashMap<Int, ByteArray>>()

    //TODO return to this and see if it's the best solution to concurrent modification problem
    var cachedDataToMerge = ConcurrentHashMap<Long, ConcurrentHashMap<Int, T>>()
    val pagesToRemove: MutableList<Long> = Collections.synchronizedList(mutableListOf<Long>())
    val pageIndicesToRemove = ConcurrentHashMap<Long, ConcurrentSkipListSet<Int>>()
    val cachedData = mutableMapOf<Long, MutableMap<Int, T>>()

    override fun close() {
        serverChecksums.clear()
        clientChecksums.clear()
        cachedData.clear()
        pagesToRemove.clear()
        cachedDataToMerge.clear()
        pageIndicesToRemove.clear()
    }

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

    fun mergeData() {
        if (pageIndicesToRemove.isNotEmpty()) {
            synchronized(pageIndicesToRemove) {
            synchronized(clientChecksums) {
            synchronized(serverChecksums) {
                for ((pageNum, indices) in pageIndicesToRemove) {
                    val page = cachedData[pageNum] ?: continue
                    val cpage = clientChecksums[pageNum]
                    val spage = serverChecksums[pageNum]
                    for (idx in indices) {
                        page.remove(idx)
                        cpage?.remove(idx)
                        spage?.remove(idx)
                    }
                }
                pageIndicesToRemove.clear()
            }}}
        }
        if (pagesToRemove.isNotEmpty()) {
            synchronized(pagesToRemove) {
                for (pageNum in pagesToRemove) {
                    cachedData.remove(pageNum)
                }
                pagesToRemove.clear()
            }
        }
        if (cachedDataToMerge.isNotEmpty()) {
            synchronized(cachedDataToMerge) {
                for ((pageNum, page) in cachedDataToMerge) {
                    for ((k, item) in page) {
                        cachedData.getOrPut(pageNum) { mutableMapOf() }[k] = item
                    }
                }
                cachedDataToMerge.clear()
            }
        }
    }

    fun requestChecksumsUpdate(page: Long) {
        dataRequestChecksumConnection().sendToServer(ClientDataRequestPacket(page))
    }

    fun requestUpdateData(page: Long) {
        val serverPage = serverChecksums[page]!!
        val clientPage = clientChecksums.getOrPut(page) { ConcurrentHashMap() }

        val serverIds = serverPage.keys
        var clientIds = clientPage.keys

        clientIds.filter { !clientIds.containsAll(serverIds) }.forEach {clientPage.remove(it)}
        clientIds = clientPage.keys

        val toUpdate = serverPage.filter { (k, v) -> !clientPage[k].contentEquals(v) }.map { it.key }.toMutableList()

        if (toUpdate.isEmpty()) { return }

        dataUpdateRequestConnection().sendToServer(ClientDataUpdateRequestPacket(page, toUpdate))
    }

    class ServerDataResponseConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerDataRequestResponsePacket>(id, "server_data_request_response_packet") {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerDataRequestResponsePacket(buf)
            if (!packet.pageExists) {
                clientInstance.serverChecksums.remove(packet.page)
                clientInstance.clientChecksums.remove(packet.page)
                clientInstance.pagesToRemove  .add(packet.page)
                return
            }

            clientInstance.serverChecksums[packet.page] = ConcurrentHashMap(packet.checksums.toMap().toMutableMap())
        }
    }

    class ServerDataUpdateRequestResponseConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerDataUpdateRequestResponsePacket<T>>(id, "server_data_update_request_response_packet") {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerDataUpdateRequestResponsePacket<T>(buf)
            if (!packet.pageExists) {
                clientInstance.serverChecksums.remove(packet.page)
                clientInstance.clientChecksums.remove(packet.page)
                clientInstance.pagesToRemove  .add(packet.page)
                return
            }
            val page = clientInstance.cachedDataToMerge.getOrPut(packet.page) { ConcurrentHashMap() }
            val toRemove = clientInstance.pageIndicesToRemove.getOrPut(packet.page) { ConcurrentSkipListSet() }
            val checksumPage = clientInstance.clientChecksums.getOrPut(packet.page) { ConcurrentHashMap() }
            val serverChecksumPage = clientInstance.serverChecksums.getOrPut(packet.page) { ConcurrentHashMap() }
            packet.newData.forEach { (idx, item) ->
                page[idx] = item
                checksumPage[idx] = item.hash()
                serverChecksumPage[idx] = item.hash()
            }
            packet.nullData.forEach { idx ->
                toRemove.add(idx)
                checksumPage.remove(idx)
                serverChecksumPage.remove(idx)
            }
        }
    }

    class ServerChecksumsUpdatedConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerChecksumsUpdatedPacket>(id, "server_checksums_updated_packet") {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerChecksumsUpdatedPacket(buf)
            val pageNum = packet.page
            val wasRemoved = packet.wasRemoved
            if (wasRemoved) {
                clientInstance.serverChecksums.remove(pageNum)
                clientInstance.clientChecksums.remove(pageNum)
                clientInstance.pagesToRemove  .add(pageNum)
                return
            }

            val page = clientInstance.serverChecksums.getOrPut(pageNum) { ConcurrentHashMap() }
            packet.updatedIndices.forEach {
                if (it.second.isEmpty()) {
                    page.remove(it.first)
                } else {
                    page[it.first] = it.second
                }
            }
        }
    }

    infix fun <TT: Serializable> String.idWithConn(constructor: (String, ClientSynchronisedData<T>) -> S2CConnection<TT>): S2CConnection<TT> {
        val instance = constructor(this, this@ClientSynchronisedData)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}