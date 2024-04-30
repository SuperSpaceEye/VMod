package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.*
import org.lwjgl.opengl.GL11
import java.awt.Color

class TimedA2BRenderer(): BaseRenderer, TimedRenderer, PositionDependentRenderer {
    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    var color: Color = Color(0)

    var width: Double = .2

    override var timestampOfBeginning: Long = -1
    override var activeFor_ms: Long = -1
    override var wasActivated: Boolean = false
    override var renderingPosition: Vector3d = Vector3d()

    constructor(point1: Vector3d,
                point2: Vector3d,
                color: Color,
                width: Double,

                timestampOfBeginning: Long,
                activeFor_ms: Long,
                renderingPosition: Vector3d
    ): this() {
        this.point1 = point1
        this.point2 = point2
        this.color = color
        this.width = width

        this.timestampOfBeginning = timestampOfBeginning
        this.activeFor_ms = activeFor_ms
        this.renderingPosition = renderingPosition
    }

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()

        val light = Int.MAX_VALUE

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = point1 - cameraPos
        val tpos2 = point2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.makeFlatRectFacingCamera(
            vBuffer, matrix,
            color.red, color.green, color.blue, color.alpha, light, width,
            tpos1, tpos2
        )

        tesselator.end()

        poseStack.popPose()
    }

    override val typeName: String = "TimedA2BRenderer"

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeColor(color)
        buf.writeDouble(width)
        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        buf.writeLong(timestampOfBeginning)
        buf.writeLong(activeFor_ms)
        buf.writeVector3d(renderingPosition)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        color = buf.readColor()
        width = buf.readDouble()
        point1 = buf.readVector3d()
        point2 = buf.readVector3d()

        timestampOfBeginning = buf.readLong()
        activeFor_ms = buf.readLong()
        renderingPosition = buf.readVector3d()
    }
}