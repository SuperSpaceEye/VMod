package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

class RopeRenderer(): BaseRenderer, AutoSerializable {
    @JsonIgnore private var i = 0

    var shipId1: Long by get(i++, -1L)
    var shipId2: Long by get(i++, -1L)

    var point1: Vector3d by get(i++, Vector3d())
    var point2: Vector3d by get(i++, Vector3d())

    var length: Double by get(i++, 0.0)

    var width: Double by get(i++, .2)
    var segments: Int by get(i++, 16)

    constructor(shipId1: Long,
                shipId2: Long,
                point1: Vector3d,
                point2: Vector3d,
                length: Double,
                width: Double,
                segments: Int
        ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.point1 = point1
        this.point2 = point2
        this.length = length
        this.width = width
        this.segments = segments
    }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
        val level = Minecraft.getInstance().level!!

        val ship1 = if (shipId1 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId1) ?: return } else null
        val ship2 = if (shipId2 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId2) ?: return } else null

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)
        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 0f, 0f, 1f)
        }

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.drawRope(
            vBuffer, matrix,
            255, 0, 0, 255, 255,
            width, segments, length,
            tpos1, tpos2
        )

        tesselator.end()
        poseStack.popPose()

        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, oldCenter: Vector3d, newCenter: Vector3d): BaseRenderer? {
        val spoint1 = if (shipId1 != -1L) {updatePosition(point1, oldCenter, newCenter)} else {Vector3d(point1)}
        val spoint2 = if (shipId2 != -1L) {updatePosition(point2, oldCenter, newCenter)} else {Vector3d(point2)}

        val newId1 = if (shipId1 != -1L) {oldToNew[shipId1]!!.id} else {-1}
        val newId2 = if (shipId2 != -1L) {oldToNew[shipId2]!!.id} else {-1}

        return RopeRenderer(newId1, newId2, spoint1, spoint2, length, width, segments)
    }

    override fun scaleBy(by: Double) {
        width *= by
        length *= by
    }
}