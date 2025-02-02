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
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.SerializableItem.get
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

class ConeBlockRenderer(): BlockRenderer {
    class State: AutoSerializable {
        @JsonIgnore private var i = 0

        var shipId: Long by get(i++, -1L)
        var pos: Vector3d by get(i++, Vector3d())
        var rot: Quaterniond by get(i++, Quaterniond())
        var scale: Float by get(i++, 1.0f)
        var color: Color by get(i++, Color(255, 255, 255))
    }
    val state = State()

    override fun serialize() = state.serialize()
    override fun deserialize(buf: FriendlyByteBuf) = state.deserialize(buf)

    inline var shipId get() = state.shipId; set(value) {state.shipId = value}
    inline var pos get() = state.pos; set(value) {state.pos = value}
    inline var rot get() = state.rot; set(value) {state.rot = value}
    inline var scale get() = state.scale; set(value) {state.scale = value}
    inline var color get() = state.color; set(value) {state.color = value}

    constructor(pos: Vector3d, rot: Quaterniond, scale: Float, shipId: ShipId, color: Color = Color(255, 255, 255)): this() {
        this.pos = pos
        this.rot = rot
        this.scale = scale
        this.shipId = shipId
        this.color = color
    }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource, timestamp: Long) {
        val level = Minecraft.getInstance().level!!

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

        val combinedLightIn = LightTexture.pack(0, level.getBrightness(LightLayer.SKY, rpoint.toBlockPos()))
        val combinedOverlayIn = OverlayTexture.NO_OVERLAY

        RenderingStuff.renderSingleBlock(A.testState, poseStack, buffer, combinedLightIn, combinedOverlayIn, if (timestamp < highlightTimestamp) Color(255, 0, 0) else color)

        poseStack.popPose()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        val spoint = updatePosition(pos, oldToNew[shipId]!!)
        return ConeBlockRenderer(spoint, Quaterniond(rot), scale, oldToNew[shipId]!!.id)
    }

    override fun scaleBy(by: Double) {
        scale *= by.toFloat()
    }
}