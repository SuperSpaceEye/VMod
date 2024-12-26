package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.*
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import java.util.*

fun closestPointOnALineToAnotherPoint(originPoint: Vector3d, linePoint1: Vector3d, linePoint2: Vector3d): Vector3d {
    val wdir = linePoint2 - linePoint1
    val t = (originPoint - linePoint1).dot(wdir) / wdir.dot(wdir)
    return linePoint1 + wdir * t
}

class PhysgunRayRenderer: BaseRenderer, TimedRenderer, PositionDependentRenderer {
    class State: AutoSerializable {
        var player: UUID by get(0, UUID(0L, 0L))
        var shipId: Long by get(1, -1L)
        var hitPosInShipyard: Vector3d by get(2, Vector3d())
        var timestampOfBeginning: Long by get(4, -1)
        var activeFor_ms: Long by get(5, Long.MAX_VALUE)
    }
    val state = State()

    override var timestampOfBeginning get() = state.timestampOfBeginning; set(value) {state.timestampOfBeginning = value}
    override val activeFor_ms get() = state.activeFor_ms

    override fun serialize() = state.serialize()
    override fun deserialize(buf: FriendlyByteBuf) = state.deserialize(buf)

    override val renderingPosition: Vector3d
        get() {
            val player = Minecraft.getInstance().level!!.getPlayerByUUID(state.player) ?: return Vector3d(999999999, 999999999, 999999999)
            return Vector3d(player.eyePosition)
        }
    override var wasActivated: Boolean = false

    fun lerp(a: Vector3d, b: Vector3d, f: Double) = a * (1.0 - f) + b * f

    fun quadBeizer(start: Vector3d, middle: Vector3d, stop: Vector3d, f: Double): Vector3d {
        val a = lerp(start, middle, f)
        val b = lerp(middle, stop, f)

        val c = lerp(a, b, f)
        return c
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
        val targetUUID = state.player

        val player = Minecraft.getInstance().level!!.getPlayerByUUID(state.player) ?: return

        val level = player.level() as ClientLevel


        val inFirstPerson = player.uuid == targetUUID && !camera.isDetached

        val (point1, dir) = if (inFirstPerson) {
            val dir = Vector3d(camera.lookVector).snormalize()
            val pos = Vector3d(camera.position) - Vector3d(camera.upVector) * 0.2 - Vector3d(camera.leftVector) * 0.6 + dir * 0.45
            Pair(pos, dir)
        } else {
            val pos = Vector3d(player.eyePosition)
            val dir = Vector3d(player.lookAngle).snormalize()
            Pair(pos, dir)
        }

        val color = Color(0, 0, 255, 100)
        val width = 0.1

        val raycastResult = RaycastFunctions.renderRaycast(level,
            RaycastFunctions.Source(
                dir,
                if (inFirstPerson) Vector3d(camera.position) else Vector3d(player.eyePosition)
            ),
            100.0
        )

        val raycastPos = raycastResult.worldHitPos ?: (point1 + dir * 100.0)

        val point2 = if (state.shipId == -1L) {
            raycastPos
        } else {
            level.shipObjectWorld.loadedShips.getById(state.shipId)?.let {
                posShipToWorldRender(it, state.hitPosInShipyard)
            } ?: return
        }

        val linePoint = closestPointOnALineToAnotherPoint(point2, point1, raycastPos)
        val length = (linePoint - point1).dist()
        val middle = point1 + dir * (length / 2.0)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        poseStack.pushPose()

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()
        RenderSystem.enableCull()

        val light = Int.MAX_VALUE

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)

        val matrix = poseStack.last().pose()

        var up = Vector3d(camera.upVector)
        var right = -Vector3d(camera.leftVector)

        val num = 32
        var fpos = point1
        var leftPoints = RenderingUtils.Quad.makePolygon(4, width, up, right, point1)
        for (i in 1 .. num) {
            val spos = quadBeizer(point1, middle, point2, i.toDouble() / num.toDouble())

            val dir = (fpos - spos).snormalize()
            right = dir.cross(up)

            val rightPoints = RenderingUtils.Quad.makePolygon(4, width, up, right, spos)

            RenderingUtils.Quad.drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 0.0f, leftPoints, rightPoints)

            leftPoints = rightPoints
            fpos = spos
        }

        tesselator.end()
        poseStack.popPose()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        TODO("Not yet implemented")
    }

    override fun scaleBy(by: Double) {
        TODO("Not yet implemented")
    }
}