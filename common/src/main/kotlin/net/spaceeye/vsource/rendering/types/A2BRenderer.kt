package net.spaceeye.vsource.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.rendering.RenderingUtils
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.posShipToWorldRender
import net.spaceeye.vsource.utils.readVector3d
import net.spaceeye.vsource.utils.writeVector3d
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color

class A2BRenderer(): RenderingData {
    var ship1isShip: Boolean = false
    var ship2isShip: Boolean = false

    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    var color: Color = Color(0)

    var width: Double = .2

    constructor(ship1isShip: Boolean,
                ship2isShip: Boolean,
                point1: Vector3d,
                point2: Vector3d,
                color: Color,
                width: Double,
    ): this() {
        this.ship1isShip = ship1isShip
        this.ship2isShip = ship2isShip
        this.point1 = point1
        this.point2 = point2
        this.color = color
        this.width = width
    }

    override fun getTypeName() = "A2BRenderer"

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
        RenderSystem.setShader(GameRenderer::getPositionColorShader)

//        val light = LightTexture.pack(level!!.getBrightness(LightLayer.BLOCK, point1.toBlockPos()), level!!.getBrightness(LightLayer.SKY, point1.toBlockPos()))

        val light = Int.MAX_VALUE

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.makeFlatRectFacingCamera(
            vBuffer, matrix,
            color.red, color.green, color.blue, color.alpha, light, width,
            tpos1, tpos2
        )

        tesselator.end()

        poseStack.popPose()
    }

    override fun hash(): ByteArray {
        return SynchronisedRenderingData.hasher.digest(serialize().accessByteBufWithCorrectSize())
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeInt(color.rgb)
        buf.writeBoolean(ship1isShip)
        buf.writeBoolean(ship2isShip)

        buf.writeDouble(width)

        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        color = Color(buf.readInt())
        ship1isShip = buf.readBoolean()
        ship2isShip = buf.readBoolean()

        width = buf.readDouble()

        point1 = buf.readVector3d()
        point2 = buf.readVector3d()
    }
}