package net.spaceeye.vsource.rendering

import com.google.common.base.Supplier
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.DLOG
import net.spaceeye.vsource.WLOG
import net.spaceeye.vsource.constraintsManaging.ConstraintManager
import net.spaceeye.vsource.events.AVSEvents
import net.spaceeye.vsource.networking.dataSynchronization.ClientSynchronisedData
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.rendering.types.RopeRenderer
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.networking.dataSynchronization.ServerChecksumsUpdatedPacket
import net.spaceeye.vsource.networking.dataSynchronization.ServerSynchronisedData
import net.spaceeye.vsource.rendering.types.BaseRenderer
import net.spaceeye.vsource.rendering.types.TimedA2BRenderer
import org.valkyrienskies.mod.common.shipObjectWorld
import java.security.MessageDigest

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

        for ((k, v) in data) {
            val shipIdTag = CompoundTag()
            for ((id, item) in v) {
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
            save.put(k.toString(), shipIdTag)
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

        for (k in save.allKeys) {
            val shipIdTag = save.get(k) as CompoundTag
            val page = data.getOrPut(k.toLong()) { mutableMapOf() }
            for (kk in shipIdTag.allKeys) {
                val dataItemTag = shipIdTag[kk] as CompoundTag

                val typeIdx = dataItemTag.getInt("typeIdx")
                val item = RenderingTypes.idxToSupplier(typeIdx).get()
                try {
                    item.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(dataItemTag.getByteArray("data"))))
                } catch (e: Exception) {
                    WLOG("FAILED TO DESERIALIZE RENDER COMMAND OF SHIP ${page} WITH IDX ${typeIdx} AND TYPE ${item.getTypeName()}")
                    continue
                }

                page[kk.toInt()] = item
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

    var idCounter = 0

    fun addTimedRenderer(renderer: BaseRenderer) {
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
            DLOG("SENT DELETED SHIPID")
        }
    }
}