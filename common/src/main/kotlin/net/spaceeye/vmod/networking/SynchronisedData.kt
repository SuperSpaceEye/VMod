package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.Side
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.state.ConnectionNetworking.registerTR
import net.spaceeye.vmod.utils.readVarLongArray
import net.spaceeye.vmod.utils.writeVarLongArray
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

abstract class SynchronisedDataTransmitter<T: Serializable> (
    streamName: String,
    receiverSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000,
    val itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
    val itemReader: (buf: FriendlyByteBuf) -> Serializable
) {
    private val subscribersSavedChecksums = mutableMapOf<UUID, MutableMap<Long, MutableMap<Int, ByteArray>>>()

    private val data = ConcurrentHashMap<Long, MutableMap<Int, T>>()
    private val dataUpdates = ConcurrentHashMap<Long, MutableMap<Int, T?>?>()

    abstract fun sendUpdateToSubscriber(uuid: UUID, data: T2RSynchronizationTickData)

    fun get(page: Long): Map<Int, T>? {return data[page]}
    fun set(page: Long, index: Int, item: T) {
        synchronized(data) {
        synchronized(dataUpdates) {

        data.getOrPut(page) {mutableMapOf()}[index] = item

        var updatePage = dataUpdates.getOrPut(page) {mutableMapOf()}
        if (updatePage == null) {
            updatePage = mutableMapOf()
            dataUpdates[page] = updatePage
        }
        updatePage[index] = item
        }}
    }
    fun remove(page: Long, index: Int) {
        synchronized(data) {
        synchronized(dataUpdates) {
            data[page]?.remove(index) ?: return
            dataUpdates.getOrPut(page) {mutableMapOf()}?.remove(index)
        }}
    }
    fun remove(page: Long) {
        synchronized(data) {
        synchronized(dataUpdates) {
            data.remove(page)
            dataUpdates[page] = null
        }}
    }

    //TODO this is horrible
    private fun compileDifferences(from: MutableMap<Long, MutableMap<Int, ByteArray>>):
            Pair<
                MutableMap<Long, MutableMap<Int, ByteArray?>?>,
                MutableMap<Long, MutableMap<Int, T>>
                >
    {
        val newData = mutableMapOf<Long, MutableMap<Int, T>>()
        return Pair(dataUpdates.map { (page, updates) ->
            val fromState = from[page]
            // page was deleted
            if (updates == null && fromState != null) { return@map Pair(page, null) }
            // if both are null then no change
            if (updates == null) { return@map Pair(page, null) }

            return@map Pair(page, if (fromState == null) {
                // need to update everything
                newData[page] = updates.mapNotNull { (k, v) -> Pair(k, v ?: return@mapNotNull null) }.toMap().toMutableMap()
                updates.map{ (idx, value) ->  Pair(idx, value?.hash()) }.toMap().toMutableMap()
            } else {
                // update only necessary items
                updates
                    .map { (idx, value) -> Pair(idx, value?.hash()) }
                    .filter { (idx, value) -> !value.contentEquals(fromState[idx]) }
                    .associate { pair ->
                        if (pair.second != null) newData.getOrPut(page) { mutableMapOf() }[pair.first] = updates[pair.first]!!
                        pair
                    }.toMutableMap()
            })

        }.toMap().toMutableMap(), newData)
    }

    fun synchronizationTick() {
        if (dataUpdates.isEmpty()) {return}
        synchronized(data) {
        synchronized(dataUpdates) {
            subscribersSavedChecksums.forEach { (sub, checksums) ->
                val (diff, data) = compileDifferences((checksums))
                val flatCsms = diff.toList().map { (k, v) -> Pair(k, v?.toList()?.toMutableList()) }.toMutableList()
                val flatData = data.toList().map { (k, v) -> Pair(k, v.toList().toMutableList<Pair<Int, Serializable>>()) }.toMutableList()
                sendUpdateToSubscriber(sub, T2RSynchronizationTickData(itemWriter, itemReader, flatCsms, flatData))
            }
            dataUpdates.clear()
        }}
    }

    val r2tSynchronizeData = registerTR("r2t_sync_data", currentSide) {
        object : TRConnection<R2TSynchronizationTickData>(it, streamName, receiverSide) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = R2TSynchronizationTickData()
                pkt.deserialize(buf)


            }
        }
    }
}

abstract class SynchronisedDataReceiver<T: Serializable> (
    streamName: String,
    transmitterSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000,
    val itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
    val itemReader: (buf: FriendlyByteBuf) -> Serializable
) {
    private val cachedData = mutableMapOf<Long, MutableMap<Int, T>>()
    @Volatile private var dataChanged = false

    //if item null then remove it
    private val newData = ConcurrentHashMap<Long, ConcurrentHashMap<Int, T?>?>()

    val serverChecksums = ConcurrentHashMap<Long, ConcurrentHashMap<Int, ByteArray>>()
    val cachedChecksums = ConcurrentHashMap<Long, ConcurrentHashMap<Int, ByteArray>>()

    fun getData(): Map<Long, Map<Int, T>> {
        if (!dataChanged) {return cachedData}

        synchronized(newData) {
            newData.forEach { (page, updates) ->
                if (updates == null) {cachedData.remove(page); return@forEach}
                val dataPage = cachedData.getOrPut(page) {mutableMapOf()}
                updates.forEach{ (idx, item) ->
                    if (item == null) {
                        dataPage.remove(idx)
                    } else {
                        dataPage[idx] = item
                    }
                }
            }
            newData.clear()
            dataChanged = false
        }

        return cachedData
    }

    abstract fun sendRequestToServer(req: R2TSynchronizationTickData)

    val pageChecksumsToUpdate = ConcurrentLinkedDeque<Long>()
    val pageDataToUpdate = ConcurrentHashMap<Long, MutableSet<Int>>()
    val pagesToUpdateSubscriptionTo = ConcurrentLinkedDeque<Pair<Long, Boolean>>()

    fun requestChecksumUpdate(page: Long) {pageChecksumsToUpdate.add(page)}
    fun requestDataUpdate(page: Long, index: Int) {pageDataToUpdate.getOrPut(page){ mutableSetOf() }.add(index)}

    fun subscribeToPageUpdates(page: Long) {pagesToUpdateSubscriptionTo.add(Pair(page, true))}
    fun unsubscribeFromPageUpdates(page: Long) {pagesToUpdateSubscriptionTo.add(Pair(page, false))}

    // call when receiver is allowed to request
    fun synchronizationTick() {
        synchronized(pageChecksumsToUpdate) {
        synchronized(pageDataToUpdate) {
        synchronized(pagesToUpdateSubscriptionTo) {

        if (pageChecksumsToUpdate.isEmpty()
         && pageDataToUpdate.isEmpty()
         && pagesToUpdateSubscriptionTo.isEmpty()) {
            return
        }

        sendRequestToServer(R2TSynchronizationTickData(
            pagesToUpdateSubscriptionTo.toMutableList(),
            pageChecksumsToUpdate.toMutableList(),
            pageDataToUpdate.toList().map {Pair(it.first, it.second.toMutableList())}.toMutableList()
        ))

        }}}
    }

    val t2rSynchronizeData = registerTR("t2r_sync_data", currentSide) {
        object : TRConnection<T2RSynchronizationTickData>(it, streamName, transmitterSide) {
            override fun handlerFn(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = T2RSynchronizationTickData(itemWriter, itemReader)
                pkt.deserialize(buf)

                synchronized(newData) {
                synchronized(serverChecksums) {
                synchronized(cachedChecksums) {

                pkt.dataUpdates.forEach { (page, updates) ->
                    val dataPage = newData  .getOrPut(page) {ConcurrentHashMap()}!!
                    val sc = serverChecksums.getOrPut(page) {ConcurrentHashMap()}
                    val cc = cachedChecksums.getOrPut(page) {ConcurrentHashMap()}
                    updates.forEach {(idx, item) ->
                        dataPage[idx] = item as T
                        val hash = item.hash()
                        sc[idx] = hash
                        cc[idx] = hash
                    }
                }

                pkt.checksumsUpdates.forEach { (page, updates) ->
                    if (updates == null) {
                        serverChecksums.remove(page)
                        cachedChecksums.remove(page)
                        newData[page] = null
                        return@forEach
                    }
                    var newPage = newData.getOrPut(page) {ConcurrentHashMap()}
                    if (newPage == null) {
                        newPage = ConcurrentHashMap()
                        newData[page] = newPage
                    }
                    updates.forEach { (idx, item) ->
                        if (item == null) {
                            serverChecksums[page]?.remove(idx)
                            cachedChecksums[page]?.remove(idx)
                            newPage[idx] = null
                            return@forEach
                        }
                        serverChecksums.getOrPut(page) {ConcurrentHashMap()}[idx] = item
                    }
                }
                dataChanged = true
                }}}
            }
        }
    }
}

class R2TSynchronizationTickData(): Serializable {
    var subscriptions = mutableListOf<Pair<Long, Boolean>>()
    var checksumsToUpdate = mutableListOf<Long>()
    var pageDataToUpdate = mutableListOf<Pair<Long, MutableList<Int>>>()

    constructor(
        _subscriptions: MutableList<Pair<Long, Boolean>>,
        _checksumsToUpdate: MutableList<Long>,
        _pageDataToUpdate: MutableList<Pair<Long, MutableList<Int>>>
    ): this() {
        subscriptions = _subscriptions
        checksumsToUpdate = _checksumsToUpdate
        pageDataToUpdate = _pageDataToUpdate
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeCollection(subscriptions) {buf, it -> buf.writeVarLong(it.first); buf.writeBoolean(it.second)}
        buf.writeVarLongArray(checksumsToUpdate)
        buf.writeCollection(pageDataToUpdate) {buf, it -> buf.writeVarLong(it.first); buf.writeVarIntArray(it.second.toIntArray())}

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        subscriptions = buf.readCollection({mutableListOf()}) {buf -> Pair(buf.readVarLong(), buf.readBoolean())}
        checksumsToUpdate = buf.readVarLongArray()
        pageDataToUpdate = buf.readCollection({mutableListOf()}) {buf -> Pair(buf.readVarLong(), buf.readVarIntArray().toMutableList())}
    }
}

class T2RSynchronizationTickData(
    val itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
    val itemReader: (buf: FriendlyByteBuf) -> Serializable
): Serializable {
    var checksumsUpdates = mutableListOf<Pair<Long, MutableList<Pair<Int, ByteArray?>>?>>()
    var dataUpdates = mutableListOf<Pair<Long, MutableList<Pair<Int, Serializable>>>>()

    constructor(
        itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
        itemReader: (buf: FriendlyByteBuf) -> Serializable,
        _checksumsUpdates: MutableList<Pair<Long, MutableList<Pair<Int, ByteArray?>>?>>,
        _dataUpdates: MutableList<Pair<Long, MutableList<Pair<Int, Serializable>>>>
    ): this(itemWriter, itemReader) {
        checksumsUpdates = _checksumsUpdates
        dataUpdates = _dataUpdates
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeCollection(dataUpdates) {buf, item ->
            buf.writeVarLong(item.first)
            buf.writeCollection(item.second) {buf, item ->
                buf.writeVarInt(item.first)
                itemWriter(buf, item.second)
            }
        }

        buf.writeCollection(checksumsUpdates) {buf, item ->
            buf.writeVarLong(item.first)
            buf.writeBoolean(item.second != null)
            if (item.second == null) {return@writeCollection}
            buf.writeCollection(item.second!!) {buf, item ->
                buf.writeVarInt(item.first)
                buf.writeBoolean(item.second != null)
                buf.writeByteArray(item.second!!)
            }
        }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        dataUpdates = buf.readCollection({mutableListOf()}) { buf ->
            Pair(buf.readVarLong(), buf.readCollection({mutableListOf()}) {
                Pair(buf.readVarInt(), itemReader(buf))
            })
        }

        checksumsUpdates = buf.readCollection({mutableListOf()}) {buf ->
            Pair(buf.readVarLong(), if (!buf.readBoolean()) {null} else {
                buf.readCollection({mutableListOf()}) {buf ->
                Pair(buf.readVarInt(), if (!buf.readBoolean()) {null} else {
                    buf.readByteArray()
                })
            }})
        }
    }
}