package net.spaceeye.vmod.rendering.types.special

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
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.createSpacedPoints
import net.spaceeye.vmod.utils.vs.*
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color

class PrecisePlacementAssistRenderer(
    var precisePlacementAssistSideNum: Int
): BaseRenderer {
    val level = Minecraft.getInstance().level!!

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
        if (!ToolgunItem.playerIsUsingToolgun()) {return}

        val raycastResult = RaycastFunctions.renderRaycast(level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            ),
            20.0
        )

        val state = raycastResult.state
        if (state.isAir) {return}

        val centered = raycastResult.worldCenteredHitPos!!
        val worldNormal = raycastResult.worldNormalDirection!!
        val globalNormal = raycastResult.globalNormalDirection!!
        val point = raycastResult.worldHitPos!! + raycastResult.worldNormalDirection!! * 0.01
        val ship = level.getShipManagingPos(raycastResult.blockPosition) as ClientShip?

        var up = when {
            globalNormal.x > 0.5 || globalNormal.x < -0.5 -> Vector3d(0, 1, 0)
            globalNormal.y > 0.5 || globalNormal.y < -0.5 -> Vector3d(1, 0, 0)
            globalNormal.z > 0.5 || globalNormal.z < -0.5 -> Vector3d(0, 1, 0)
            else -> throw AssertionError("impossible")
        }

        var right = when {
            globalNormal.x > 0.5 || globalNormal.x < -0.5 -> Vector3d(0, 0, 1)
            globalNormal.y > 0.5 || globalNormal.y < -0.5 -> Vector3d(0, 0, 1)
            globalNormal.z > 0.5 || globalNormal.z < -0.5 -> Vector3d(1, 0, 0)
            else -> throw AssertionError("impossible")
        }

        ship?.let { up = transformDirectionShipToWorldRender(it, up); right = transformDirectionShipToWorldRender(it, right) }

        val points = createSpacedPoints(centered, up, right, 1.0, precisePlacementAssistSideNum).reduce { acc, vector3ds -> acc.addAll(vector3ds); acc }

        val closest = points.minBy { (it - point).sqrDist() }



        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()
        RenderSystem.disableCull()

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        poseStack.pushPose()

        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)

        val tup = up * 0.05
        val tright = right * 0.05
        val tworldNormal = worldNormal * 0.01

        for (point in points) {
            //TODO add color options to a config or smth idfk
            val color = if (point == closest) { Color(0, 255, 0) } else { Color(0, 0, 255, 100) }

            RenderingUtils.Quad.drawQuad(vBuffer, poseStack.last().pose(), color.red, color.green, color.blue, color.alpha, 240,
                point + tup - tright + tworldNormal,
                point - tup - tright + tworldNormal,
                point - tup + tright + tworldNormal,
                point + tup + tright + tworldNormal
            )
        }

        tesselator.end()
        poseStack.popPose()
        RenderSystem.enableCull()
    }

    override fun serialize(): FriendlyByteBuf { throw AssertionError("Shouldn't be serialized") }
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}