package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager.Side
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.utils.readVarLongArray
import net.spaceeye.vmod.utils.writeVarLongArray
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

typealias ChecksumType = ByteArray

abstract class SynchronisedDataTransmitter<T> (
    streamName: String,
    receiverSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000
) {
    private val subscribersSavedChecksums = mutableMapOf<UUID, MutableMap<Long, MutableMap<Int, ChecksumType>>>()
    private val data = ConcurrentHashMap<Long, MutableMap<Int, T>>()

    private val dataUpdates = ConcurrentHashMap<Long, MutableMap<Int, T>>()

    // items SHOULD BE CHANGED BY CALLING SET
    fun get(page: Long): Map<Int, T>? {return data[page]}
    fun set(page: Long, index: Int, item: T) {
        dataUpdates.getOrPut(page) { mutableMapOf() }[index] = item
        data.getOrPut(page) { mutableMapOf() }[index] = item
    }

    fun synchronizationTick() {
        if (dataUpdates.isEmpty()) {return}
        synchronized(dataUpdates) {

        }
    }
}

abstract class SynchronisedDataReceiver<T> (
    streamName: String,
    transmitterSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000
) {
    private val cachedData = mutableMapOf<Long, MutableMap<Int, T>>()
    private var dataChanged = false

    fun getData(): Map<Long, Map<Int, T>> {
        if (!dataChanged) {return cachedData}
        //TODO execute cleanup in here
        return cachedData
    }

    abstract fun sendRequestToServer(req: R2CSynchronizationTickData)

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

        sendRequestToServer(R2CSynchronizationTickData(
            pagesToUpdateSubscriptionTo.toMutableList(),
            pageChecksumsToUpdate.toMutableList(),
            pageDataToUpdate.toList().map {Pair(it.first, it.second.toMutableList())}.toMutableList()
        ))

        }}}
    }
}

class R2CSynchronizationTickData(): Serializable {
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