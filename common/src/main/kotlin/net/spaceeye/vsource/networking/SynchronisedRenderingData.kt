package net.spaceeye.vsource.networking

import com.google.common.base.Supplier
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import io.netty.buffer.Unpooled
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.rendering.RenderingUtils
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.utils.dataSynchronization.ClientSynchronisedData
import net.spaceeye.vsource.utils.dataSynchronization.DataUnit
import net.spaceeye.vsource.utils.dataSynchronization.ServerChecksumsUpdatedPacket
import net.spaceeye.vsource.utils.dataSynchronization.ServerSynchronisedData
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.security.MessageDigest

fun mixinRunFn(poseStack: PoseStack, camera: Camera) {
    val level = Minecraft.getInstance().level!!
    for (ship in level.shipObjectWorld.loadedShips) {
        SynchronisedRenderingData.clientSynchronisedData.tryPoolDataUpdate(ship.id)
        for ((idx, render) in SynchronisedRenderingData.clientSynchronisedData.cachedData[ship.id] ?: continue) {
            render.renderData(poseStack, camera)
        }
    }
}

object RenderingTypes {
    private val strToIdx = mutableMapOf<String, Int>()
    private val suppliers = mutableListOf<Supplier<RenderingData>>()

    init {
        register { SimpleRopeRenderer() }
    }

    private fun register(supplier: Supplier<RenderingData>) {
        suppliers.add(supplier)
        strToIdx[supplier.get().getTypeName()] = suppliers.size - 1
    }

    fun typeToIdx(type: String) = strToIdx[type]
    fun idxToSupplier(idx: Int) = suppliers[idx]
}

interface RenderingData: DataUnit {
    fun renderData(poseStack: PoseStack, camera: Camera)
}

class SimpleRopeRenderer(): RenderingData {
    var ship1isShip: Boolean = false
    var ship2isShip: Boolean = false

    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    constructor(ship1isShip: Boolean,
                ship2isShip: Boolean,
                point1: Vector3d,
                point2: Vector3d): this() {
        this.ship1isShip = ship1isShip
        this.ship2isShip = ship2isShip
        this.point1 = point1
        this.point2 = point2
    }

    override fun getTypeName() = "SimpleRopeRendering"

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val level = Minecraft.getInstance().level

        val ship1 = level.getShipManagingPos(point1.toBlockPos())
        val ship2 = level.getShipManagingPos(point2.toBlockPos())

        //I don't think VS reuses shipyard plots so
        if (ship1isShip && ship1 == null) {return}
        if (ship2isShip && ship2 == null) {return}
//        if (!ship1isShip && ship1 != null) {return}
//        if (!ship2isShip && ship2 != null) {return}

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1 as ClientShip, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2 as ClientShip, point2)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)

//                RenderSystem.disableDepthTest()
//                RenderSystem.disableBlend()
//                RenderSystem.disableCull()
//                RenderSystem.disableScissor()

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.makeFlatRectFacingCamera(vBuffer, matrix,
            255, 0, 0, 255, 255, .2,
            tpos1, tpos2)

        tesselator.end()

        poseStack.popPose()
    }

    override fun hash(): ByteArray {
        return SynchronisedRenderingData.hasher.digest(serialize().accessByteBufWithCorrectSize())
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeBoolean(ship1isShip)
        buf.writeBoolean(ship2isShip)

        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        ship1isShip = buf.readBoolean()
        ship2isShip = buf.readBoolean()

        point1 = buf.readVector3d()
        point2 = buf.readVector3d()
    }
}

class ClientSynchronisedRenderingData(getServerInstance: () -> ServerSynchronisedData<RenderingData>): ClientSynchronisedData<RenderingData>("rendering_data", getServerInstance)
class ServerSynchronisedRenderingData(getClientInstance: () -> ClientSynchronisedData<RenderingData>): ServerSynchronisedData<RenderingData>("rendering_data", getClientInstance) {
    //TODO switch from nbt to just directly writing to byte buffer?
    fun nbtSave(tag: CompoundTag): CompoundTag {
        val save = CompoundTag()

        for ((k, v) in data) {
            val shipIdTag = CompoundTag()
            for ((id, item) in v) {
                val dataItemTag = CompoundTag()

                val typeIdx = RenderingTypes.typeToIdx(item.getTypeName())
                if (typeIdx == null) {
                    LOG("RENDERING ITEM WITH TYPE ${item.getTypeName()} RETURNED NULL TYPE INDEX DURING SAVING")
                    continue
                }

                dataItemTag.putInt("typeIdx", typeIdx)
                dataItemTag.putByteArray("data", item.serialize().accessByteBufWithCorrectSize())

                shipIdTag.put(id.toString(), dataItemTag)
            }
            save.put(k.toString(), shipIdTag)
        }

        tag.put("server_synchronised_data_${id}", save)
        return tag
    }

    fun nbtLoad(tag: CompoundTag) {
        if (!tag.contains("server_synchronised_data_${id}")) {return}
        val save = tag.get("server_synchronised_data_${id}") as CompoundTag

        for (k in save.allKeys) {
            val shipIdTag = save.get(k) as CompoundTag
            val page = data.getOrPut(k.toLong()) { mutableMapOf() }
            for (kk in shipIdTag.allKeys) {
                val dataItemTag = shipIdTag[kk] as CompoundTag

                val typeIdx = dataItemTag.getInt("typeIdx")
                val item = RenderingTypes.idxToSupplier(typeIdx).get()
                item.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(dataItemTag.getByteArray("data"))))

                page[kk.toInt()] = item
            }
        }
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
                    ServerLevelHolder.serverLevel!!.server.playerList.players,
                    ServerChecksumsUpdatedPacket(shipData.id, true)
                )
            LOG("SENT DELETED SHIPID")
        }
    }
}