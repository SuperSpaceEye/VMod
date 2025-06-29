package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.Side
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.readVarLongArray
import net.spaceeye.vmod.utils.writeVarLongArray
import java.lang.IllegalStateException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

//TODO make more abstract
abstract class SynchronisedDataTransmitter<T: Serializable> (
    streamName: String,
    receiverSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000,
    val itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
    val itemReader: (buf: FriendlyByteBuf) -> Serializable
) {
    private val subscribersSavedChecksums = mutableMapOf<UUID, MutableMap<Long, MutableMap<Int, ByteArray>>>()

    private val data = mutableMapOf<Long, MutableMap<Int, T>>()
    private val dataUpdates = mutableMapOf<Long, MutableMap<Int, T?>?>()

    private val uuidToPlayer = mutableMapOf<UUID, Player?>()

    protected val lock = ReentrantLock()
    protected var counter = 0

    val allIds: Set<Int>
        get() {
            return mutableSetOf<Int>().also { set -> data.forEach { set.addAll(it.value.keys) } }
        }


    protected fun close() {
        subscribersSavedChecksums.clear()
        data.clear()
        dataUpdates.clear()
        uuidToPlayer.clear()
        counter = 0
    }

//    abstract fun sendUpdateToSubscriber(ctx: NetworkManager.PacketContext, data: T2RSynchronizationTickData)

    protected inline fun <T>lock(fn: () -> T): T {
        synchronized(lock) {
            return fn()
        }
    }

    fun get(page: Long): Map<Int, T>? = data[page]

    fun add(page: Long, item: T): Int = lock {
        set(page, counter, item)
        return counter-1
    }
    fun add(pages: Set<Long>, item: T): Int = lock {
        set(pages, counter, item)
        return counter-1
    }

    fun set(page: Long, index: Int, item: T) = lock {
        data.getOrPut(page) { mutableMapOf() }[index] = item

        var updatePage = dataUpdates.getOrPut(page) { mutableMapOf() }
        if (updatePage == null) {
            updatePage = mutableMapOf()
            dataUpdates[page] = updatePage
        }
        updatePage[index] = item

        counter = max(index + 1, counter)
    }

    fun set(pages: Set<Long>, index: Int, item: T) = lock {
        pages.forEach { page -> set(page, index, item) }
    }

    fun remove(page: Long, index: Int): Boolean = lock {
        data[page]?.remove(index) ?: return false
        dataUpdates.getOrPut(page) {mutableMapOf()}?.set(index, null)
        return true
    }

    fun remove(page: Long) = lock {
        data.remove(page)
        dataUpdates[page] = null
    }

    fun subscribeTo(uuid: UUID, player: Player, page: Long) {
        uuidToPlayer[uuid] = player
        subscribersSavedChecksums.getOrPut(uuid) {mutableMapOf()}.getOrPut(page) {mutableMapOf()}
    }

    fun removeSubscriber(uuid: UUID) {
        uuidToPlayer.remove(uuid)
        subscribersSavedChecksums.remove(uuid)
    }

    private fun compileDifferences(origin: MutableMap<Long, MutableMap<Int, T?>?>, against: MutableMap<Long, MutableMap<Int, ByteArray>>):
        Pair<
            MutableMap<Long, MutableMap<Int, ByteArray?>?>,
            MutableMap<Long, MutableMap<Int, T>>
            >
    {
        val newData = mutableMapOf<Long, MutableMap<Int, T>>()
        return Pair(origin.mapNotNull { (page, updates) ->
            val fromState = against[page] ?: return@mapNotNull null
            // page was deleted
            if (updates == null) { return@mapNotNull Pair(page, null) }

            return@mapNotNull Pair(page,
                updates
                    .map { (idx, value) -> Pair(idx, value?.hash()) }
                    .filter { (idx, value) -> !value.contentEquals(fromState[idx]) }
                    .associate { pair ->
                        if (pair.second != null) newData.getOrPut(page) { mutableMapOf() }[pair.first] = updates[pair.first]!!
                        pair
                    }.toMutableMap()
            )

        }.toMap().toMutableMap(), newData)
    }

    private fun compileUpdateDataForSubscriber(
        origin: MutableMap<Long, MutableMap<Int, T?>?>,
        checksums: MutableMap<Long, MutableMap<Int, ByteArray>>): T2RSynchronizationTickData? {
        val (diff, data) = compileDifferences(origin, checksums)
        if (diff.isEmpty() && data.isEmpty()) { return null }
        diff.forEach { (page, updates) ->
            if (updates == null) { checksums.remove(page); return@forEach }
            val line = checksums.getOrPut(page) { mutableMapOf() }
            updates.forEach { (idx, item) ->
                if (item == null) { line.remove(idx); return@forEach }
                line[idx] = item
            }
        }

        val removedPages = mutableListOf<Long>()

        var idToPageCsms = mutableMapOf<Int, Pair<ByteArray?, MutableList<Long>>>()
        diff.forEach { (page, idCsm) -> idCsm?.forEach { (id, csm) -> idToPageCsms.getOrPut(id) { Pair(csm, mutableListOf()) }.second.add(page) } ?: removedPages.add(page) }

        var idToPageItem = mutableMapOf<Int, Pair<Serializable, MutableList<Long>>>()
        data.forEach { (page, idCsm) -> idCsm.forEach { (id, csm) -> idToPageItem.getOrPut(id) { Pair(csm, mutableListOf()) }.second.add(page) } }

        return T2RSynchronizationTickData(itemWriter, itemReader, removedPages, idToPageCsms, idToPageItem)
    }

    fun synchronizationTick() {
        if (dataUpdates.isEmpty()) {return}
        lock {
            subscribersSavedChecksums.forEach { (sub, checksums) ->
                val res = compileUpdateDataForSubscriber(dataUpdates, checksums) ?: return@forEach
                trSynchronizeData.startSendingDataToReceiver(res, FakePacketContext((uuidToPlayer[sub] ?: return@forEach) as ServerPlayer))
            }
            dataUpdates.clear()
        }
    }

    internal val trSynchronizeData = object : DataStream<R2TSynchronizationTickData, T2RSynchronizationTickData>(
        streamName,
        receiverSide.opposite(),
        currentSide,
        VM.MOD_ID
    ) {
        override val partByteAmount: Int = partByteMaxAmount
        override fun requestPacketConstructor(buf: FriendlyByteBuf): R2TSynchronizationTickData = R2TSynchronizationTickData()
        override fun dataPacketConstructor(): T2RSynchronizationTickData = T2RSynchronizationTickData(itemWriter, itemReader)

        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { throw AssertionError() }
        override fun receiverDataTransmitted(uuid: UUID, data: T2RSynchronizationTickData, ctx: NetworkManager.PacketContext) { throw AssertionError() }

        override fun transmitterRequestProcessor(pkt: R2TSynchronizationTickData, ctx: NetworkManager.PacketContext): Either<T2RSynchronizationTickData, RequestFailurePkt>? {
            val uuid = ctx.player.uuid
            uuidToPlayer[uuid] = ctx.player
            val checksums = subscribersSavedChecksums.getOrPut(uuid) {mutableMapOf()}
            val tempData = (data as MutableMap<Long, MutableMap<Int, T?>?>).toMutableMap()

            pkt.checksumsToUpdate.forEach { checksums[it] = mutableMapOf() }
            pkt.pageDataToUpdate.forEach { (page, line) ->
                val dataLine = checksums.getOrPut(page) {mutableMapOf()}
                line.forEach { idx ->
                    dataLine[idx] = byteArrayOf()
                }
            }
            pkt.subscriptions.forEach { (page, subscribe) ->
                when (subscribe) {
                    true -> checksums[page] = mutableMapOf()
                    false -> {
                        checksums.remove(page)
                        tempData.remove(page)
                        subscribersSavedChecksums[uuid]?.remove(page)
                    }
                }
            }

            val res = compileUpdateDataForSubscriber(tempData, subscribersSavedChecksums[uuid]!!) ?: return null
            return Either.Left(res)
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
    protected val cachedData = mutableMapOf<Long, MutableMap<Int, T>>()
    @Volatile private var dataChanged = false

    //if item null then remove it
    private val newData = mutableMapOf<Long, MutableMap<Int, T?>?>()

    private val serverChecksums = mutableMapOf<Long, MutableMap<Int, ByteArray>>()
    private val cachedChecksums = mutableMapOf<Long, MutableMap<Int, ByteArray>>()

    val lock = ReentrantLock()

    protected open fun onClear() {}
    protected open fun onRemove(page: Long) {}
    protected open fun onRemove(page: Long, idx: Int) {}
    protected open fun onAdd(page: Long, idx: Int, item: T) {}

    inline fun <T>lock(fn: () -> T): T {
        synchronized(lock) {
            return fn()
        }
    }

    protected fun clear() = lock {
        cachedData.clear()
        dataChanged = false
        newData.clear()
        serverChecksums.clear()
        cachedChecksums.clear()

        onClear()
    }

    protected fun remove(page: Long) = lock {
        onRemove(page)
        cachedData.remove(page)
        newData.remove(page)
        serverChecksums.remove(page)
        cachedChecksums.remove(page)
    }

    protected fun remove(page: Long, idx: Int) = lock {
        onRemove(page, idx)
        cachedData[page]?.remove(idx)
        newData[page]?.remove(idx)
        serverChecksums[page]?.remove(idx)
        cachedChecksums[page]?.remove(idx)
    }

    protected fun add(page: Long, item: T): Int = lock {
        val pageData = cachedData[page]
        val key = pageData?.keys?.maxOrNull()?.let { it + 1 } ?: 0
        set(page, key, item)
        onAdd(page, key, item)
        return key
    }

    protected fun set(page: Long, idx: Int, item: T) = lock {
        cachedData.getOrPut(page) {mutableMapOf()}[idx] = item
    }

    fun getData(): Map<Long, Map<Int, T>> {
        if (!dataChanged) {return cachedData}

        lock {
            newData.forEach { (page, updates) ->
                if (updates == null) {cachedData.remove(page); onRemove(page); return@forEach}
                val dataPage = cachedData.getOrPut(page) {mutableMapOf()}
                updates.forEach { (idx, item) ->
                    if (item == null) {
                        dataPage.remove(idx)
                        onRemove(page, idx)
                    } else {
                        dataPage[idx] = item
                        onAdd(page, idx, item)
                    }
                }
            }
            newData.clear()
            dataChanged = false
        }

        return cachedData
    }

//    abstract fun sendRequestToServer(req: R2TSynchronizationTickData)

    private val pageChecksumsToUpdate = ConcurrentLinkedDeque<Long>()
    private val pageDataToUpdate = ConcurrentHashMap<Long, MutableSet<Int>>()
    private val pagesToUpdateSubscriptionTo = ConcurrentLinkedDeque<Pair<Long, Boolean>>()

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

        try {
        trSynchronizeData.r2tRequestData.transmitData(R2TSynchronizationTickData(
            pagesToUpdateSubscriptionTo.toMutableList(),
            pageChecksumsToUpdate.toMutableList(),
            pageDataToUpdate.toList().map {Pair(it.first, it.second.toMutableList())}.toMutableList()
        ))
        pageChecksumsToUpdate.clear()
        pageDataToUpdate.clear()
        pagesToUpdateSubscriptionTo.clear()
        //will happen when user exits the server so ignore it
        } catch (_: IllegalStateException) {}
        }}}
    }

    internal val trSynchronizeData = object : DataStream<R2TSynchronizationTickData, T2RSynchronizationTickData>(
        streamName,
        transmitterSide,
        currentSide,
        VM.MOD_ID
    ) {
        override val partByteAmount: Int = partByteMaxAmount
        override fun requestPacketConstructor(buf: FriendlyByteBuf): R2TSynchronizationTickData = R2TSynchronizationTickData()
        override fun dataPacketConstructor(): T2RSynchronizationTickData = T2RSynchronizationTickData(itemWriter, itemReader)
        override fun transmitterRequestProcessor(req: R2TSynchronizationTickData, ctx: NetworkManager.PacketContext): Either<T2RSynchronizationTickData, RequestFailurePkt>? { throw AssertionError() }
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("IT SHOULDN'T BE POSSIBLE TO REACH THIS. HOW DID YOU DO THIS.") }

        override fun receiverDataTransmitted(uuid: UUID, pkt: T2RSynchronizationTickData, ctx: NetworkManager.PacketContext) {
            lock {
                pkt.dataUpdates.forEach { (idx, pair) ->
                    val (item, pages) = pair
                    pages.forEach { page ->
                        val dataPage = newData  .getOrPut(page) {mutableMapOf()}!!
                        val sc = serverChecksums.getOrPut(page) {mutableMapOf()}
                        val cc = cachedChecksums.getOrPut(page) {mutableMapOf()}

                        dataPage[idx] = item as T
                        val hash = item.hash()
                        sc[idx] = hash
                        cc[idx] = hash
                    }
                }

                pkt.checksumsUpdates.forEach { (idx, pair) ->
                    val (csm, pages) = pair
                    if (csm != null) {
                        pages.forEach { page -> serverChecksums.getOrPut(page) {mutableMapOf()}[idx] = csm }
                        return@forEach
                    }
                    pages.forEach { page ->
                        serverChecksums[page]?.remove(idx)
                        cachedData[page]?.remove(idx)
                        newData
                            .getOrPut(page) {mutableMapOf()}
                            .let { it ?: mutableMapOf<Int, T?>().also { newData[page] = it } }
                            .set(idx, null)
                    }
                }

                pkt.removedPages.forEach { page ->
                    serverChecksums.remove(page)
                    cachedChecksums.remove(page)
                    newData[page] = null
                }

                dataChanged = true
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
    var removedPages = longArrayOf()
    var checksumsUpdates = mutableListOf<Pair<Int, Pair<ByteArray?, MutableList<Long>>>>()
    var dataUpdates = mutableListOf<Pair<Int, Pair<Serializable, MutableList<Long>>>>()

    constructor(
        itemWriter: (buf: FriendlyByteBuf, item: Serializable) -> Unit,
        itemReader: (buf: FriendlyByteBuf) -> Serializable,
        removedPages: MutableList<Long>,
        checksumsUpdates: MutableMap<Int, Pair<ByteArray?, MutableList<Long>>>,
        dataUpdates: MutableMap<Int, Pair<Serializable, MutableList<Long>>>
    ): this(itemWriter, itemReader) {
        this.removedPages = removedPages.toLongArray()
        this.checksumsUpdates = checksumsUpdates.toList().toMutableList()
        this.dataUpdates = dataUpdates.toList().toMutableList()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLongArray(removedPages)

        buf.writeCollection(dataUpdates) {buf, (index, pair) ->
            buf.writeVarInt(index)
            itemWriter(buf, pair.first)
            buf.writeVarLongArray(pair.second)
        }

        buf.writeCollection(checksumsUpdates) { buf, (index, pair) ->
            val (csm, pages) = pair

            buf.writeVarInt(index)
            buf.writeBoolean(csm != null)
            csm?.let { buf.writeByteArray(it) }
            buf.writeVarLongArray(pages)
        }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        removedPages = buf.readLongArray()

        dataUpdates = buf.readCollection({mutableListOf()}) { buf ->
            Pair(buf.readVarInt(), Pair(
                 itemReader(buf),
                 buf.readVarLongArray()
            ))
        }

        checksumsUpdates = buf.readCollection({mutableListOf()}) { buf ->
            Pair(   buf.readVarInt(), Pair(
                if (buf.readBoolean())
                   {buf.readByteArray()} else {null},
                    buf.readVarLongArray()
            ))
        }
    }
}