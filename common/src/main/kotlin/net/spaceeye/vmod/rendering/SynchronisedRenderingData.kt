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
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.rendering.types.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*

internal fun serializeItem(buf: FriendlyByteBuf, item: Serializable) {
    buf.writeInt(RenderingTypes.typeToIdx(item::class.java as Class<out BaseRenderer>)!!)
    buf.writeByteArray(item.serialize().array())
}

internal fun deserializeItem(buf: FriendlyByteBuf): Serializable {
    val item = RenderingTypes.idxToSupplier(buf.readInt()).get()
    item.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray())))
    return item
}

class ClientSynchronisedRenderingData(streamName: String = "rendering_data"):
    SynchronisedDataReceiver<BaseRenderer>(
        streamName,
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

    val s2cSetSchema = regS2C<ServerSetRenderingSchemaPacket>(MOD_ID, "rendering_data", "client_synchronised") { pkt ->
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
            synchronizeUpdates()
            worldRenderingData.synchronizeUpdates()
            worldRenderingData.completeSynchronize(worldRenderingData.playerUpdates)
        }
    }

    internal val worldRenderingData = ServerWorldSynchronisedRenderingData()

    private var idToPages = mutableMapOf<Int, Set<Long>>()

    fun setUpdated(id: Int, renderer: BaseRenderer): Boolean = lock {
        val pages = idToPages[id] ?: return@lock false
        if (pages.contains(ReservedRenderingPages.WorldRenderingObject)) {
            if (renderer !is PositionDependentRenderer) throw RuntimeException("World Renderers should implement PositionDependentRenderer")
            return worldRenderingData.setRenderer(id, renderer) != null
        }
        set(pages, id, renderer)
        return true
    }

    fun setRenderer(shipIds: List<ShipId>, id: Int, renderer: BaseRenderer): Int = lock {
        val idsToUse = shipIds.filter { it != -1L }.also { if (it.isEmpty()) {
            if (renderer !is PositionDependentRenderer) throw RuntimeException("World Renderers should implement PositionDependentRenderer")
            worldRenderingData.setRenderer(id, renderer) ?: return id
            idToPages[id] = setOf(ReservedRenderingPages.WorldRenderingObject)
            return id
        } }.toSet()
        set(idsToUse, id, renderer)
        idToPages[id] = idsToUse
        return id
    }

    fun addRenderer(shipIds: List<ShipId>, renderer: BaseRenderer, dimensionId: DimensionId? = null): Int = lock {
        val idsToUse = shipIds.filter { it != -1L }.also { if (it.isEmpty() || (it.size == 1 && it.contains(-1L))) {
            if (renderer !is PositionDependentRenderer) throw RuntimeException("World Renderers should implement PositionDependentRenderer")
            if (dimensionId == null) throw RuntimeException("World Renderers need non-null dimensionId")
            val id = worldRenderingData.addRenderer(dimensionId, renderer)
            idToPages[id] = setOf(ReservedRenderingPages.WorldRenderingObject)
            return id
        } }.toSet()
        val id = add(idsToUse, renderer)
        idToPages[id] = idsToUse
        return id
    }

    fun removeRenderer(id: Int): Boolean = lock {
        val pageIds = idToPages[id] ?: return false
        idToPages.remove(id)

        if (pageIds.contains(ReservedRenderingPages.WorldRenderingObject)) {
            return worldRenderingData.removeRenderer(id) != null
        }
        return pageIds.map { pageId -> remove(pageId, id) }.any { it }
    }

    fun getRenderer(id: Int): BaseRenderer? = lock {
        val pagesId = idToPages[id] ?: return null
        if (pagesId.contains(ReservedRenderingPages.WorldRenderingObject)) {
            return worldRenderingData.getRenderer(id)
        }
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

    var worldClientSynchronisedRenderingData = ClientSynchronisedRenderingData("world_rendering_data")

    init {
        makeServerEvents()
        makeClientEvents()
    }

    private fun makeClientEvents() {
        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            //TODO could be abused to get info on ships not in player's FOV
            vsApi.shipLoadEventClient.on { event -> val ship = event.ship
                clientSynchronisedData.subscribeToPageUpdates(ship.id)
            }
            vsApi.shipUnloadEventClient.on { event, _ -> val ship = event.ship
                clientSynchronisedData.unsubscribeFromPageUpdates(ship.id)
            }

            AVSEvents.clientPhysEntityLoad.on { data, _ ->
                clientSynchronisedData.subscribeToPageUpdates(data.id)
            }
            AVSEvents.clientPhysEntityUnload.on { id, _ ->
                clientSynchronisedData.unsubscribeFromPageUpdates(id)
            }

            AVSEvents.clientBodyLoad.on { data, _ ->
                clientSynchronisedData.subscribeToPageUpdates(data.id)
            }
            AVSEvents.clientBodyUnload.on { id, _ ->
                clientSynchronisedData.unsubscribeFromPageUpdates(id)
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
            worldClientSynchronisedRenderingData.s2cSetSchema.sendToClient(it, ServerSetRenderingSchemaPacket(RenderingTypes.getSchema()))
            serverSynchronisedData.subscribePlayerToReservedPages(it)
        }
        PlayerEvent.PLAYER_QUIT.register {
            serverSynchronisedData.removeSubscriber(it.uuid)
            serverSynchronisedData.worldRenderingData.removeSubscriber(it.uuid)
        }
    }
}

object RenderingData {
    val client get() = SynchronisedRenderingData.clientSynchronisedData
    val server get() = SynchronisedRenderingData.serverSynchronisedData
    val clientWorld get() = SynchronisedRenderingData.worldClientSynchronisedRenderingData
}

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