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
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.rendering.types.*
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
        addCustomClient { clear() }
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
    override fun onAdd(page: Long, idx: Int, item: BaseRenderer) { idToItem[idx] = item }

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
        addCustomServer { close(); idToPage.clear() }
        TickEvent.SERVER_PRE.register {
            synchronizationTick()
        }
    }

    private var idToPage = mutableMapOf<Int, Long>()

    fun setUpdated(id: Int, renderer: BaseRenderer): Boolean = lock {
        val page = idToPage[id] ?: return@lock false
        set(page, id, renderer)
        return@lock true
    }

    fun setRenderer(shipId1: Long, shipId2: Long, id: Int, renderer: BaseRenderer): Int = lock {
        val idToUse = if (ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.containsValue(shipId1)) {shipId2} else {shipId1}
        set(idToUse, id, renderer)
        idToPage[id] = idToUse
        return id
    }

    fun addRenderer(shipId1: Long, shipId2: Long, renderer: BaseRenderer): Int = lock {
        val idToUse = if (ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.containsValue(shipId1)) {shipId2} else {shipId1}
        val id = add(idToUse, renderer)
        idToPage[id] = idToUse
        return id
    }

    fun removeRenderer(id: Int): Boolean = lock {
        val pageId = idToPage[id] ?: return false
        return remove(pageId, id)
    }

    fun getRenderer(id: Int): BaseRenderer? = lock {
        val pageId = idToPage[id] ?: return null
        return get(pageId)?.get(id)
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