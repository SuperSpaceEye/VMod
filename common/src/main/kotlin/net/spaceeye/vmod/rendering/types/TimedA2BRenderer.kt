package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.*
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

class TimedA2BRenderer(): BaseRenderer, TimedRenderer, PositionDependentRenderer {
    class State: AutoSerializable {
        var point1: Vector3d by get(0, Vector3d())
        var point2: Vector3d by get(1, Vector3d())

        var color: Color by get(2, Color(0))

        var width: Double by get(3, .2)

        var timestampOfBeginning: Long by get(4, -1)
        var activeFor_ms: Long by get(5, -1)
        var renderingPosition: Vector3d by get(6, Vector3d())
    }

    val state = State()

    inline var point1 get() = state.point1; set(value) {state.point1 = value}
    inline var point2 get() = state.point2; set(value) {state.point2 = value}
    inline var color get() = state.color; set(value) {state.color = value}
    inline var width get() = state.width; set(value) {state.width = value}
    override var timestampOfBeginning get() = state.timestampOfBeginning; set(value) {state.timestampOfBeginning = value}
    override var activeFor_ms get() = state.activeFor_ms; set(value) {state.activeFor_ms = value}
    override var renderingPosition get() = state.renderingPosition; set(value) {state.renderingPosition = value}

    override fun serialize() = state.serialize()
    override fun deserialize(buf: FriendlyByteBuf) = state.deserialize(buf)

    override var wasActivated: Boolean = false

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

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
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

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}