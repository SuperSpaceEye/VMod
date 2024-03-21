package net.spaceeye.vmod.rendering

import com.google.common.base.Supplier
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.DLOG
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.networking.dataSynchronization.ClientSynchronisedData
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.networking.dataSynchronization.ServerChecksumsUpdatedPacket
import net.spaceeye.vmod.networking.dataSynchronization.ServerSynchronisedData
import net.spaceeye.vmod.rendering.ReservedRenderingPages.reservedPagesList
import net.spaceeye.vmod.rendering.types.*
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import java.security.MessageDigest
import java.util.ConcurrentModificationException

object RenderingTypes {
    private val strToIdx = mutableMapOf<String, Int>()
    private val suppliers = mutableListOf<Supplier<BaseRenderer>>()

    init {
        register { RopeRenderer() }
        register { A2BRenderer() }
        register { TimedA2BRenderer() }
    }

    private fun register(supplier: Supplier<BaseRenderer>) {
        suppliers.add(supplier)
        strToIdx[supplier.get().getTypeName()] = suppliers.size - 1
    }

    fun typeToIdx(type: String) = strToIdx[type]
    fun idxToSupplier(idx: Int) = suppliers[idx]
}

class ClientSynchronisedRenderingData(getServerInstance: () -> ServerSynchronisedData<BaseRenderer>): ClientSynchronisedData<BaseRenderer>("rendering_data", getServerInstance)
class ServerSynchronisedRenderingData(getClientInstance: () -> ClientSynchronisedData<BaseRenderer>): ServerSynchronisedData<BaseRenderer>("rendering_data", getClientInstance) {
    //TODO switch from nbt to just directly writing to byte buffer?
    fun nbtSave(tag: CompoundTag): CompoundTag {
        val save = CompoundTag()

        DLOG("SAVING RENDERING DATA")
        val point = getNow_ms()

        for ((pageId, items) in data) {
            if (reservedPagesList.contains(pageId)) { continue }
            if (!ConstraintManager.allShips!!.contains(pageId)) { continue }

            val shipIdTag = CompoundTag()
            for ((id, item) in items) {
                val dataItemTag = CompoundTag()

                val typeIdx = RenderingTypes.typeToIdx(item.getTypeName())
                if (typeIdx == null) {
                    WLOG("RENDERING ITEM WITH TYPE ${item.getTypeName()} RETURNED NULL TYPE INDEX DURING SAVING")
                    continue
                }

                dataItemTag.putInt("typeIdx", typeIdx)
                dataItemTag.putByteArray("data", item.serialize().accessByteBufWithCorrectSize())

                shipIdTag.put(id.toString(), dataItemTag)
            }
            save.put(pageId.toString(), shipIdTag)
        }

        DLOG("FINISHING SAVING RENDERING DATA IN ${getNow_ms() - point} ms")

        tag.put("server_synchronised_data_${id}", save)
        return tag
    }

    fun nbtLoad(tag: CompoundTag) {
        if (!tag.contains("server_synchronised_data_${id}")) {return}
        val save = tag.get("server_synchronised_data_${id}") as CompoundTag

        DLOG("LOADING RENDERING DATA")
        val point = getNow_ms()

        for (shipId in save.allKeys) {
            val shipIdTag = save.get(shipId) as CompoundTag
            val page = data.getOrPut(shipId.toLong()) { mutableMapOf() }
            for (id in shipIdTag.allKeys) {
                val dataItemTag = shipIdTag[id] as CompoundTag

                val typeIdx = dataItemTag.getInt("typeIdx")
                val item = RenderingTypes.idxToSupplier(typeIdx).get()
                try {
                    item.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(dataItemTag.getByteArray("data"))))
                } catch (e: Exception) {
                    WLOG("FAILED TO DESERIALIZE RENDER COMMAND OF SHIP ${page} WITH IDX ${typeIdx} AND TYPE ${item.getTypeName()}")
                    continue
                }

                page[id.toInt()] = item
                idToPage[id.toInt()] = shipId.toLong()
            }
        }
        DLOG("FINISHING LOADING RENDERING DATA in ${getNow_ms() - point} ms")
    }

    var idToPage = mutableMapOf<Int, Long>()

    //TODO think of a better way to expose this
    fun addRenderer(shipId1: Long, shipId2: Long, id: Int, renderer: BaseRenderer) {
        data.getOrPut(shipId2) { mutableMapOf() }
        data.getOrPut(shipId1) { mutableMapOf() }
        val idToUse = if (ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.containsValue(shipId1)) {shipId2} else {shipId1}
        val page = data[idToUse]!!
        page[id] = renderer

        idToPage[id] = idToUse

        ConstraintManager.getInstance().setDirty()

        serverChecksumsUpdatedConnection().sendToClients(ServerLevelHolder.server!!.playerList.players, ServerChecksumsUpdatedPacket(
            idToUse, mutableListOf(Pair(id, renderer.hash()))
        ))
    }

    fun removeRenderer(id: Int): Boolean {
        val pageId = idToPage[id] ?: return false
        val page = data[pageId] ?: return false
        page.remove(id)

        serverChecksumsUpdatedConnection().sendToClients(ServerLevelHolder.server!!.playerList.players, ServerChecksumsUpdatedPacket(
            pageId, mutableListOf(Pair(id, byteArrayOf()))
        ))

        return true
    }

    fun getRenderer(id: Int): BaseRenderer? {
        val pageId = idToPage[id] ?: return null
        val page = data[pageId] ?: return null
        return page[id]
    }

    var idCounter = 0

    private fun trimTimedRenderers() {
        val page = data[ReservedRenderingPages.TimedRenderingObjects] ?: return
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
            toRemove.forEach { page.remove(it) }
        } catch (e: ConcurrentModificationException) {return}
    }

    //TODO trim timed objects
    fun addTimedRenderer(renderer: BaseRenderer) {
        trimTimedRenderers()
        val page = data.getOrPut(ReservedRenderingPages.TimedRenderingObjects) { mutableMapOf() }
        page[idCounter] = renderer

        serverChecksumsUpdatedConnection().sendToClients(ServerLevelHolder.server!!.playerList.players, ServerChecksumsUpdatedPacket(
            ReservedRenderingPages.TimedRenderingObjects, mutableListOf(Pair(idCounter, renderer.hash()))
        ))
        idCounter++
    }

    override fun close() {
        super.close()
        idCounter = 0
        idToPage.clear()
    }
}

object SynchronisedRenderingData {
    lateinit var clientSynchronisedData: ClientSynchronisedRenderingData
    lateinit var serverSynchronisedData: ServerSynchronisedRenderingData

    //MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
    val hasher = MessageDigest.getInstance("MD5")

    init {
        clientSynchronisedData = ClientSynchronisedRenderingData { serverSynchronisedData }
        serverSynchronisedData = ServerSynchronisedRenderingData { clientSynchronisedData }
        makeServerEvents()
    }

    private fun makeServerEvents() {
        AVSEvents.serverShipRemoveEvent.on {
            (shipData), handler ->
            serverSynchronisedData.data.remove(shipData.id)
            serverSynchronisedData.serverChecksumsUpdatedConnection()
                .sendToClients(
                    ServerLevelHolder.overworldServerLevel!!.server.playerList.players,
                    ServerChecksumsUpdatedPacket(shipData.id, true)
                )
        }
        VSEvents.shipLoadEvent.on {
            (shipData), handler ->
            serverSynchronisedData.data.getOrPut(shipData.id) { mutableMapOf() }
        }
    }
}