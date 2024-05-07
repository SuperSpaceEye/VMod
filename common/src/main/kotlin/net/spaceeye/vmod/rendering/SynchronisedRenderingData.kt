package net.spaceeye.vmod.rendering

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.dataSynchronization.*
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.rendering.types.*
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import java.security.MessageDigest
import java.util.ConcurrentModificationException

object RenderingTypes: Registry<BaseRenderer>() {
    init {
        register(::RopeRenderer)
        register(::A2BRenderer)
        register(::TimedA2BRenderer)
        register(::PhysRopeRenderer)
    }
}

class ClientSynchronisedRenderingData(getServerInstance: () -> ServerSynchronisedData<BaseRenderer>): ClientSynchronisedData<BaseRenderer>("rendering_data", getServerInstance) {
    val setSchema = "rendering_data" idWithConn ::ServerSetRenderingSchemaConnection

    class ServerSetRenderingSchemaConnection<T: DataUnit>(id: String, val clientInstance: ClientSynchronisedData<T>): S2CConnection<ServerSetRenderingSchemaPacket>("server_set_rendering_schema", id) {
        override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
            val packet = ServerSetRenderingSchemaPacket(buf)
            RenderingTypes.setSchema(packet.schema.map { Pair(it.value, it.key) }.toMap())
        }
    }
}
class ServerSynchronisedRenderingData(getClientInstance: () -> ClientSynchronisedData<BaseRenderer>): ServerSynchronisedData<BaseRenderer>("rendering_data", getClientInstance) {
    var idToPage = mutableMapOf<Int, Long>()

    //TODO think of a better way to expose this
    fun addRenderer(shipId1: Long, shipId2: Long, id: Int, renderer: BaseRenderer) {
        synchronized(data) {
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
    }

    fun removeRenderer(id: Int): Boolean {
        synchronized(data) {
        val pageId = idToPage[id] ?: return false
        val page = data[pageId] ?: return false
        page.remove(id)

        serverChecksumsUpdatedConnection().sendToClients(ServerLevelHolder.server!!.playerList.players, ServerChecksumsUpdatedPacket(
            pageId, mutableListOf(Pair(id, byteArrayOf()))
        ))

        return true
        }
    }

    fun getRenderer(id: Int): BaseRenderer? {
        synchronized(data) {
        val pageId = idToPage[id] ?: return null
        val page = data[pageId] ?: return null
        return page[id]
        }
    }

    var idCounter = 0

    private fun trimTimedRenderers() {
        synchronized(data) {
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
    }

    //TODO trim timed objects
    fun addTimedRenderer(renderer: BaseRenderer) {
        synchronized(data) {
        trimTimedRenderers()
        val page = data.getOrPut(ReservedRenderingPages.TimedRenderingObjects) { mutableMapOf() }
        page[idCounter] = renderer

        serverChecksumsUpdatedConnection().sendToClients(ServerLevelHolder.server!!.playerList.players, ServerChecksumsUpdatedPacket(
            ReservedRenderingPages.TimedRenderingObjects, mutableListOf(Pair(idCounter, renderer.hash()))
        ))
        idCounter++
        }
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
    val hasher: MessageDigest = MessageDigest.getInstance("MD5")

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

        PlayerEvent.PLAYER_JOIN.register {
            clientSynchronisedData.setSchema.sendToClient(it, ServerSetRenderingSchemaPacket(RenderingTypes.getSchema()))
        }
    }
}