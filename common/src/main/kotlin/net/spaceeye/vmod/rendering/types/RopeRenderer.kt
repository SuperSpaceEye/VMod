package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.readVector3d
import net.spaceeye.vmod.utils.writeVector3d
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.common.getShipManagingPos

class RopeRenderer(): BaseRenderer {
    var ship1isShip: Boolean = false
    var ship2isShip: Boolean = false

    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    var length: Double = 0.0

    var width: Double = .2
    var segments: Int = 16

    constructor(ship1isShip: Boolean,
                ship2isShip: Boolean,
                point1: Vector3d,
                point2: Vector3d,
                length: Double,
                width: Double,
                segments: Int
        ): this() {
        this.ship1isShip = ship1isShip
        this.ship2isShip = ship2isShip
        this.point1 = point1
        this.point2 = point2
        this.length = length
        this.width = width
        this.segments = segments
    }

    override val typeName = "RopeRendering"

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val level = Minecraft.getInstance().level

        val ship1 = level.getShipManagingPos(point1.toBlockPos())
        val ship2 = level.getShipManagingPos(point2.toBlockPos())

        if (ship1isShip && ship1 == null) {return}
        if (ship2isShip && ship2 == null) {return}

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1 as ClientShip, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2 as ClientShip, point2)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.drawRope(
            vBuffer, matrix,
            255, 0, 0, 255, 255,
            width, segments, length,
            tpos1, tpos2
        )

        tesselator.end()

        poseStack.popPose()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeBoolean(ship1isShip)
        buf.writeBoolean(ship2isShip)

        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        buf.writeDouble(length)

        buf.writeDouble(width)
        buf.writeInt(segments)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        ship1isShip = buf.readBoolean()
        ship2isShip = buf.readBoolean()

        point1 = buf.readVector3d()
        point2 = buf.readVector3d()

        length = buf.readDouble()

        width = buf.readDouble()
        segments = buf.readInt()
    }

    override fun copy(nShip1: Ship?, nShip2: Ship?, spoint1: Vector3d, spoint2: Vector3d): BaseRenderer {
        return RopeRenderer(nShip1 != null, nShip2 != null, spoint1, spoint2, length, width, segments)
    }
}