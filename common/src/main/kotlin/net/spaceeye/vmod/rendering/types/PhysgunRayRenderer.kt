package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderSetups
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.*
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

class PhysgunRayRenderer: BaseRenderer(), TimedRenderer, PositionDependentRenderer, ReflectableObject {
    class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var player: UUID by get(i++, UUID(0L, 0L))
        var shipId: Long by get(i++, -1L)
        var hitPosInShipyard: Vector3d by get(i++, Vector3d())
        var timestampOfBeginning: Long by get(i++, -1)
        var activeFor_ms: Long by get(i++, Long.MAX_VALUE)
    }
    var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }
    override var timestampOfBeginning: Long get() = data.timestampOfBeginning; set(value) {data.timestampOfBeginning = value}
    override val activeFor_ms: Long get() = data.activeFor_ms

    override val renderingPosition: Vector3d
        get() = with(data) {
            val player = Minecraft.getInstance().level!!.getPlayerByUUID(player) ?: return Vector3d(999999999, 999999999, 999999999)
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

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) = with(data) {
        val selfPlayer = Minecraft.getInstance().player
        val player = Minecraft.getInstance().level!!.getPlayerByUUID(player) ?: return

        val level = player.level() as ClientLevel

        val inFirstPerson = player.uuid == selfPlayer?.uuid && !camera.isDetached

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

        val point2 = if (shipId == -1L) {
            raycastPos
        } else {
            level.shipObjectWorld.loadedShips.getById(shipId)?.let {
                posShipToWorldRender(it, hitPosInShipyard)
            } ?: return
        }

        val linePoint = closestPointOnALineToAnotherPoint(point2, point1, raycastPos)
        val length = (linePoint - point1).dist()
        val middle = point1 + dir * (length / 2.0)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        poseStack.pushPose()

        val light = LightTexture.FULL_BRIGHT
        vBuffer.begin(VertexFormat.Mode.QUADS, RenderSetups.setupPCRendering())
        RenderSystem.enableCull()

        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)

        val matrix = poseStack.last().pose()

        var up = Vector3d(camera.upVector)
        var right = -Vector3d(camera.leftVector)

        val num = 32
        var fpos = point1
        var leftPoints = RenderingUtils.Quad.makePolygonPoints(4, width, up, right, point1)
        for (i in 1 .. num) {
            val spos = quadBeizer(point1, middle, point2, i.toDouble() / num.toDouble())

            val dir = (fpos - spos).snormalize()
            right = dir.cross(up)

            val rightPoints = RenderingUtils.Quad.makePolygonPoints(4, width, up, right, spos)

            RenderingUtils.Quad.drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, light, 0.0f, 1.0f, 0f, 1.0f, leftPoints, rightPoints)

            leftPoints = rightPoints
            fpos = spos
        }

        tesselator.end()
        poseStack.popPose()

        RenderSetups.clearPCRendering()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = throw AssertionError("shouldn't be copied")
    override fun scaleBy(by: Double) = throw AssertionError("shouldn't be scaled")
}