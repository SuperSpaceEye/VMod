package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.VMBlocks
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderingStuff
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.joml.Quaterniond
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color


object A {
    val testState = VMBlocks.CONE_THRUSTER.get().defaultBlockState()
}

class ConeBlockRenderer(): BlockRenderer(), ReflectableObject {
    private class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var shipId: Long by get(i++, -1L)
        var pos: Vector3d by get(i++, Vector3d())
        var rot: Quaterniond by get(i++, Quaterniond())
        var scale: Float by get(i++, 1.0f, true) { ClientLimits.instance.blockRendererScale.get(it) }
        var color: Color by get(i++, Color(255, 255, 255))
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    constructor(
        pos: Vector3d,
        rot: Quaterniond,
        scale: Float,
        shipId: ShipId,
        color: Color = Color(255, 255, 255),
        fullbright: Boolean
    ): this() { with(data) {
        this.pos = pos
        this.rot = rot
        this.scale = scale
        this.shipId = shipId
        this.color = color
        this.fullbright = fullbright
    } }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    private var highlightTick = 0L
    override fun highlightUntilRenderingTicks(until: Long) {
        if (until > highlightTick) highlightTick = until
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource, timestamp: Long) = with(data) {
        val level = Minecraft.getInstance().level!!
        val scale = scale

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionShader)
        RenderSystem.enableBlend()

        val ship = (level.getShipManagingPos(pos.toBlockPos()) ?: return) as ClientShip
        val shipScale = ship.renderTransform.shipToWorldScaling.x()
        val rpoint = posShipToWorldRender(ship, pos)

        poseStack.pushPose()

        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(rpoint.x, rpoint.y, rpoint.z)

        poseStack.mulPose(
            Quaterniond()
                .mul(ship.renderTransform.shipToWorldRotation)
                .mul(rot)
                .toMinecraft()
        )

        poseStack.scale(scale, scale, scale)
        poseStack.translate(-0.5, -0.5 / scale * shipScale, -0.5)

        val light = if (fullbright) LightTexture.FULL_BRIGHT else rpoint.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
        val combinedOverlayIn = OverlayTexture.NO_OVERLAY

        RenderingStuff.renderSingleBlock(A.testState, poseStack, buffer, light, combinedOverlayIn, if (timestamp < highlightTimestamp || renderingTick < highlightTick) Color(255, 0, 0) else color)

        poseStack.popPose()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = with(data) {
        val spoint = centerPositions[shipId]!!.let { (old, new) -> updatePosition(pos, old, new)}
        return ConeBlockRenderer(spoint, Quaterniond(rot), scale, oldToNew[shipId]!!.id, color, fullbright)
    }

    override fun scaleBy(by: Double) = with(data) {
        scale *= by.toFloat()
    }
}