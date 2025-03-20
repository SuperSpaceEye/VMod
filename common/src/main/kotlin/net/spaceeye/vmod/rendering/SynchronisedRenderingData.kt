package net.spaceeye.vmod.rendering

import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.networking.NetworkManager
import dev.architectury.utils.EnvExecutor
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.rendering.types.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*

private fun serializeItem(buf: FriendlyByteBuf, item: Serializable) {
    buf.writeInt(RenderingTypes.typeToIdx(item::class.java as Class<out BaseRenderer>)!!)
    buf.writeByteArray(item.serialize().array())
}

private fun deserializeItem(buf: FriendlyByteBuf): Serializable {
    val item = RenderingTypes.idxToSupplier(buf.readInt()).get()
    item.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray())))
    return item
}

class ClientSynchronisedRenderingData:
    SynchronisedDataReceiver<BaseRenderer>(
        "rendering_data",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
        1000000,
        ::serializeItem,
        ::deserializeItem,
        ) {

    init {
        addCustomClientClosable { clear() }
        EnvExecutor.runInEnv(EnvType.CLIENT) {
            Runnable {
                ClientTickEvent.CLIENT_PRE.register {
                    synchronizationTick()
                }
            }
        }
    }

    private val idToItem = mutableMapOf<Int, BaseRenderer>()
    fun getItem(idx: Int) = idToItem[idx]

    override fun onClear() { idToItem.clear() }
    override fun onRemove(page: Long) { cachedData[page]?.forEach {idToItem.remove(it.key)} }
    override fun onRemove(page: Long, idx: Int) { idToItem.remove(idx) }
    override fun onAdd(page: Long, idx: Int, item: BaseRenderer) {
        idToItem[idx] = item
        if (item is AutoSerializable) {item.getAllReflectableItems().forEach { it.setValue(null, null, (it.metadata["verification"] as? (Any) -> Any)?.invoke(it.it!!) ?: it.it!!) }}
    }

    fun removeTimedRenderers(toRemove: List<Int>) {
        toRemove.forEach { remove(ReservedRenderingPages.TimedRenderingObjects, it) }
    }

    fun addClientsideRenderer(renderer: BaseRenderer): Int {
        return add(ReservedRenderingPages.ClientsideRenderingObjects, renderer)
    }

    fun removeClientsideRenderer(id: Int) {
        remove(ReservedRenderingPages.ClientsideRenderingObjects, id)
    }

    val s2cSetSchema = regS2C<ServerSetRenderingSchemaPacket>("rendering_data", "client_synchronised") {pkt ->
        RenderingTypes.setSchema(pkt.schema.map { Pair(it.value, it.key) }.toMap())
    }
}
class ServerSynchronisedRenderingData:
    SynchronisedDataTransmitter<BaseRenderer>(
        "rendering_data",
        NetworkManager.Side.C2S,
        NetworkManager.Side.S2C,
        1000000,
        ::serializeItem,
        ::deserializeItem
        ) {

    init {
        addCustomServerClosable { close(); idToPages.clear() }
        TickEvent.SERVER_PRE.register {
            synchronizationTick()
        }
    }

    private var idToPages = mutableMapOf<Int, Set<Long>>()
    private val groundIds: Collection<Long> get() = ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

    fun setUpdated(id: Int, renderer: BaseRenderer): Boolean = lock {
        val pages = idToPages[id] ?: return@lock false
        set(pages, id, renderer)
        return@lock true
    }

    fun setRenderer(shipIds: List<ShipId>, id: Int, renderer: BaseRenderer): Int = lock {
        val idsToUse = shipIds.filter { !groundIds.contains(it) }.also { if (it.isEmpty()) { throw NotImplementedError("World Renderers are not implemented") } }.toSet()
        set(idsToUse, id, renderer)
        idToPages[id] = idsToUse
        return id
    }

    fun addRenderer(shipIds: List<ShipId>, renderer: BaseRenderer): Int = lock {
        val idsToUse = shipIds.filter { !groundIds.contains(it) }.also { if (it.isEmpty()) { throw NotImplementedError("World Renderers are not implemented") } }.toSet()
        val id = add(idsToUse, renderer)
        idToPages[id] = idsToUse
        return id
    }

    fun removeRenderer(id: Int): Boolean = lock {
        val pageIds = idToPages[id] ?: return false
        return pageIds.map { pageId -> remove(pageId, id) }.any { it }
    }

    fun getRenderer(id: Int): BaseRenderer? = lock {
        val pagesId = idToPages[id] ?: return null
        pagesId.forEach { pageId -> get(pageId)?.get(id)?.also { return it } }
        return null
    }

    private fun trimTimedRenderers() = lock {
        val page = get(ReservedRenderingPages.TimedRenderingObjects) ?: return
        if (page.isEmpty()) { return }
        val toRemove = mutableListOf<Int>()
        val current = getNow_ms()
        try {
            for ((k, item) in page) {
                if (item !is TimedRenderer) {toRemove.add(k); continue}
                if (item.timestampOfBeginning + item.activeFor_ms < current ) {
                    toRemove.add(k)
                }
            }
            toRemove.forEach { remove(ReservedRenderingPages.TimedRenderingObjects, it) }
        } catch (e: ConcurrentModificationException) {return}

    }

    fun addTimedRenderer(renderer: BaseRenderer) = lock {
        trimTimedRenderers()
        add(ReservedRenderingPages.TimedRenderingObjects, renderer)
    }

    fun subscribePlayerToReservedPages(player: ServerPlayer) {
        ReservedRenderingPages.reservedPages.forEach {subscribeTo(player.uuid, player, it)}
    }
}

private object SynchronisedRenderingData {
    var clientSynchronisedData = ClientSynchronisedRenderingData()
    var serverSynchronisedData = ServerSynchronisedRenderingData()

    init {
        makeServerEvents()
        makeClientEvents()
    }

    private fun makeClientEvents() {
        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            VSEvents.shipLoadEventClient.on { (ship) ->
                clientSynchronisedData.subscribeToPageUpdates(ship.id)
            }
            AVSEvents.clientShipUnloadEvent.on { (ship), _ ->
                clientSynchronisedData.unsubscribeFromPageUpdates(ship?.id ?: return@on)
            }
        }}
    }

    private fun makeServerEvents() {
        AVSEvents.serverShipRemoveEvent.on {
            (shipData), handler ->
            serverSynchronisedData.remove(shipData.id)
        }

        PlayerEvent.PLAYER_JOIN.register {
            clientSynchronisedData.s2cSetSchema.sendToClient(it, ServerSetRenderingSchemaPacket(RenderingTypes.getSchema()))
            serverSynchronisedData.subscribePlayerToReservedPages(it)
        }
        PlayerEvent.PLAYER_QUIT.register {
            serverSynchronisedData.removeSubscriber(it.uuid)
        }
    }
}

val ClientRenderingData: ClientSynchronisedRenderingData
    get() = SynchronisedRenderingData.clientSynchronisedData

val ServerRenderingData: ServerSynchronisedRenderingData
    get() = SynchronisedRenderingData.serverSynchronisedData

fun initRenderingData() {
    SynchronisedRenderingData
}

class ServerSetRenderingSchemaPacket(): Serializable {
    constructor(buf: FriendlyByteBuf) : this() { deserialize(buf) }
    constructor(schema: Map<String, Int>) : this() {
        this.schema = schema
    }
    var schema = mapOf<String, Int>()

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeCollection(schema.toList()) {buf, (key, idx) -> buf.writeUtf(key); buf.writeInt(idx) }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        schema = buf.readCollection({mutableListOf<Pair<String, Int>>()}) {Pair(buf.readUtf(), buf.readInt())}.toMap()
    }
}