package net.spaceeye.vsource.networking

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.rendering.RenderingUtils
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.dataSynchronization.ClientSynchronisedData
import net.spaceeye.vsource.utils.dataSynchronization.DataUnit
import net.spaceeye.vsource.utils.dataSynchronization.ServerSynchronisedData
import net.spaceeye.vsource.utils.posShipToWorldRender
import net.spaceeye.vsource.utils.readVector3d
import net.spaceeye.vsource.utils.writeVector3d
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

class RenderingData(): DataUnit {
    var ship1isShip: Boolean = false
    var ship2isShip: Boolean = false

    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    var level: ClientLevel? = Minecraft.getInstance().level

    constructor(ship1isShip: Boolean,
                ship2isShip: Boolean,
                point1: Vector3d,
                point2: Vector3d): this() {
        this.ship1isShip = ship1isShip
        this.ship2isShip = ship2isShip
        this.point1 = point1
        this.point2 = point2
                }

    fun renderData(poseStack: PoseStack, camera: Camera) {
        if (level == null) {return}

        val ship1 = level.getShipManagingPos(point1.toBlockPos())
        val ship2 = level.getShipManagingPos(point2.toBlockPos())

        if (ship1isShip) {
            if (ship1 == null) {return}
        } else {
            if (ship1 != null) {return}
        }
        if (ship2isShip) {
            if (ship2 == null) {return}
        } else {
            if (ship2 != null) {return}
        }

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

class ClientSynchronisedRenderingData(getServerInstance: () -> ServerSynchronisedData<RenderingData>): ClientSynchronisedData<RenderingData>("rendering_data", getServerInstance, ::RenderingData) {
}

class ServerSynchronisedRenderingData(getClientInstance: () -> ClientSynchronisedData<RenderingData>): ServerSynchronisedData<RenderingData>("rendering_data", getClientInstance) {
}

object SynchronisedRenderingData {
    lateinit var clientSynchronisedData: ClientSynchronisedRenderingData
    lateinit var serverSynchronisedData: ServerSynchronisedRenderingData

    val hasher = MessageDigest.getInstance("MD5")

    init {
        clientSynchronisedData = ClientSynchronisedRenderingData { serverSynchronisedData }
        serverSynchronisedData = ServerSynchronisedRenderingData { clientSynchronisedData }
    }
}