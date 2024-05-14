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
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.*
import org.joml.Quaterniondc
import org.joml.primitives.AABBic
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import java.awt.Color

class SchemOutlinesRenderer(
    val maxObjectEdge: Vector3d,
    val rotation: Quaterniondc,
    val center: ShipTransformImpl,
    val ships: List<Pair<ShipTransform, AABBic>>
): BaseRenderer, TimedRenderer, PositionDependentRenderer {
    override val typeName get() = "SchemOutlinesRenderer"
    override var timestampOfBeginning: Long = -1
    override var activeFor_ms: Long = Long.MAX_VALUE / 2
    override var wasActivated: Boolean = false
    override val renderingPosition get() = Vector3d(Minecraft.getInstance().cameraEntity!!.position())

    val aabbPoints = mutableListOf(Vector3d(), Vector3d(), Vector3d(), Vector3d(), Vector3d(), Vector3d(), Vector3d(), Vector3d())

    val level = Minecraft.getInstance().level!!
    val raycastDistance = VMConfig.CLIENT.TOOLGUN.MAX_RAYCAST_DISTANCE

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val mode = ClientToolGunState.currentMode
        if (mode !is SchemMode) {return}
        if (!ToolgunItem.playerIsUsingToolgun()) {return}

        val raycastResult = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            ),
            raycastDistance,
            setOf(),
            {ship, dir -> transformDirectionShipToWorldRender(ship as ClientShip, dir) },
            {ship, dir -> transformDirectionWorldToShipRender(ship as ClientShip, dir) },
            {ship, pos, transform -> posShipToWorldRender(ship as ClientShip, pos, transform) },
            {ship, pos, transform -> posWorldToShipRender(ship as ClientShip, pos, transform) }
        )

        val hitPos = raycastResult.worldHitPos ?: return
        val pos = hitPos + (raycastResult.worldNormalDirection!! * Vector3d(maxObjectEdge))

        val cameraPos = Vector3d(camera.position)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()

        vBuffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR)

        poseStack.pushPose()
        val matrix = poseStack.last().pose()

        val offset = pos - cameraPos

        for ((transform, aabb) in ships) {
            val newTransform = rotateAroundCenter(center, transform, rotation)

            aabbPoints[0].set(aabb.minX(), aabb.minY(), aabb.minZ())
            aabbPoints[1].set(aabb.minX(), aabb.maxY(), aabb.minZ())
            aabbPoints[2].set(aabb.minX(), aabb.maxY(), aabb.maxZ())
            aabbPoints[3].set(aabb.minX(), aabb.minY(), aabb.maxZ())

            aabbPoints[4].set(aabb.maxX(), aabb.minY(), aabb.minZ())
            aabbPoints[5].set(aabb.maxX(), aabb.maxY(), aabb.minZ())
            aabbPoints[6].set(aabb.maxX(), aabb.maxY(), aabb.maxZ())
            aabbPoints[7].set(aabb.maxX(), aabb.minY(), aabb.maxZ())

            aabbPoints.forEachIndexed {i, it ->
                aabbPoints[i] = posShipToWorld(null, it, newTransform) + offset
            }

            RenderingUtils.Line.renderLineBox(vBuffer, matrix, Color.RED, aabbPoints)
        }

        tesselator.end()

        poseStack.popPose()
    }

    // only for internal use on client
    override fun serialize(): FriendlyByteBuf { throw AssertionError("Shouldn't be serialized") }
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
}