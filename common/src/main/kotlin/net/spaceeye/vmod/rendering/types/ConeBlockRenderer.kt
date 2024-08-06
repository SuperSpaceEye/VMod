package net.spaceeye.vmod.rendering.types

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
import net.spaceeye.vmod.rendering.RenderingStuff
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.readVector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.writeVector3d
import org.joml.Quaterniond
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.readQuatd
import org.valkyrienskies.core.util.writeQuatd
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toMinecraft


object A {
    val testState = VMBlocks.CONE_THRUSTER.get().defaultBlockState()
}

class ConeBlockRenderer(): BlockRenderer {
    var pos = Vector3d()
    var rot = Quaterniond()
    var scale: Float = 1.0f

    override val typeName = "BlockRenderer"

    constructor(_pos: Vector3d, _rot: Quaterniond, _scale: Float): this() {
        pos = _pos
        rot = _rot
        scale = _scale
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource) {
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

        RenderingStuff.blockRenderer.renderSingleBlock(A.testState, poseStack, buffer, combinedLightIn, combinedOverlayIn)

        poseStack.popPose()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeVector3d(pos)
        buf.writeQuatd(rot)
        buf.writeFloat(scale)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        pos = buf.readVector3d()
        rot = buf.readQuatd()
        scale = buf.readFloat()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        return null
    }
}