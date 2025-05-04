package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.*
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

class TimedA2BRenderer(): BaseRenderer(), TimedRenderer, PositionDependentRenderer, ReflectableObject {
    private class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var point1: Vector3d by get(i++, Vector3d())
        var point2: Vector3d by get(i++, Vector3d())

        var color: Color by get(i++, Color(0))

        var width: Double by get(i++, .2) { ClientLimits.instance.lineRendererWidth.get(it) }

        var timestampOfBeginning: Long by get(i++, -1)
        var activeFor_ms: Long by get(i++, -1)
        var renderingPosition: Vector3d by get(i++, Vector3d())
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }
    override var timestampOfBeginning: Long get() = data.timestampOfBeginning; set(value) {data.timestampOfBeginning = value}
    override val activeFor_ms: Long get() = data.activeFor_ms
    override val renderingPosition: Vector3d get() = data.renderingPosition

    override var wasActivated: Boolean = false

    constructor(point1: Vector3d,
                point2: Vector3d,
                color: Color,
                width: Double,

                timestampOfBeginning: Long,
                activeFor_ms: Long,
                renderingPosition: Vector3d
    ): this() { with(data) {
        this.point1 = point1
        this.point2 = point2
        this.color = color
        this.width = width

        this.timestampOfBeginning = timestampOfBeginning
        this.activeFor_ms = activeFor_ms
        this.renderingPosition = renderingPosition
    } }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) = with(data) {
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

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}