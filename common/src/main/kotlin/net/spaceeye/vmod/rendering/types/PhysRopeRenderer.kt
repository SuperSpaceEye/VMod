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
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.drawPolygonTube
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.makePolygon
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.joml.Quaterniond
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.physics.VSCapsuleCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

//TODO redo
class PhysRopeRenderer(): BaseRenderer(), AutoSerializable {
    @JsonIgnore private var i = 0

    var shipId1: Long by get(i++, -1L)
    var shipId2: Long by get(i++, -1)

    var point1: Vector3d by get(i++, Vector3d())
    var point2: Vector3d by get(i++, Vector3d())

    var color: Color by get(i++, Color(0))

    var sides: Int by get(i++, 8) { ClientLimits.instance.physRopeSides.get(it) }

    var shipIds: LongArray by get(i++, longArrayOf())

    constructor(
        shipId1: ShipId,
        shipId2: ShipId,
        point1: Vector3d, point2: Vector3d,
        color: Color, sides: Int,
        shipIds: List<Long>
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.point1 = point1
        this.point2 = point2

        this.color = color
        this.sides = sides

        this.shipIds = shipIds.toLongArray()
    }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
        val level = Minecraft.getInstance().level!!

        val entities = shipIds.map { (level.shipObjectWorld as ShipObjectClientWorld).physicsEntities[it] }.filterNotNull()
        if (entities.isEmpty()) { return }

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)
        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(0f, 1f, 0f, 0.5f)
        }

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        poseStack.pushPose()

        val light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, point1.toBlockPos()), level.getBrightness(LightLayer.SKY, point1.toBlockPos()))

        val cameraPos = -Vector3d(camera.position)
        poseStack.translate(cameraPos.x, cameraPos.y, cameraPos.z)

        val matrix = poseStack.last().pose()

        // ========================
//        val entities = shipIds.map { (level.shipObjectWorld as ShipObjectClientWorld).physicsEntities[it] }.filterNotNull()
//
//        entities.forEach {
//            val shape = it.collisionShapeData as VSCapsuleCollisionShapeData
//
//            val dir1 = Vector3d(shape.length + shape.radius, 0, 0)
//            val dir2 = -dir1
//
//            val pos1 = posShipToWorldRender(null, dir1, it.renderTransform) - cameraPos
//            val pos2 = posShipToWorldRender(null, dir2, it.renderTransform) - cameraPos
//
//            RenderingUtils.Quad.makeFlatRectFacingCameraTexture(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, shape.radius, pos1, pos2)
//        }
        // =======================
        val ship1 = level.shipObjectWorld.allShips.getById(shipId1)
        val ship2 = level.shipObjectWorld.allShips.getById(shipId2)

        val quat1 = Quaterniond(ship1?.renderTransform?.shipToWorldRotation ?: Quaterniond())
        val quat2 = Quaterniond(ship2?.renderTransform?.shipToWorldRotation ?: Quaterniond())

        var ppos = (if (ship1 == null) point1 else posShipToWorldRender(ship1, point1))
        var cpos = Vector3d(entities[0].renderTransform.positionInWorld)

        var up = getUpFromQuat(quat1)

        var shape = entities[0].collisionShapeData as VSCapsuleCollisionShapeData
        var rPoints: List<Vector3d>
        var lPoints = makePoints(cpos, ppos, ppos, up, shape.radius)

        for (entity in entities) {
            shape = entity.collisionShapeData as VSCapsuleCollisionShapeData

            val capsuleDir = -Vector3d(shape.length, 0, 0) * 0.5
            cpos = posShipToWorldRender(null, capsuleDir, entity.renderTransform)

            up = getUpFromQuat(entity.renderTransform.shipToWorldRotation)
            rPoints = makePoints(cpos, ppos, cpos, up, shape.radius)

            drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)

            lPoints = rPoints
            ppos = cpos
        }

        cpos = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        up = getUpFromQuat(quat2)

        rPoints = makePoints(cpos, ppos, cpos, up, shape.radius)

        drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)

        tesselator.end()
        poseStack.popPose()

        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        RenderSystem.enableCull()
    }

    private inline fun makePoints(cpos: Vector3d, ppos: Vector3d, posToUse: Vector3d, up: Vector3d, width: Double) = makePolygon(sides, width, up, (cpos - ppos).snormalize().scross(up), posToUse)

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        return PhysRopeRenderer(
            oldToNew[shipId1]?.id ?: -1,
            oldToNew[shipId2]?.id ?: -1,
            oldToNew[shipId1]?.let { updatePosition(point1, it) } ?: point1,
            oldToNew[shipId2]?.let { updatePosition(point2, it) } ?: point2,
            color, sides, listOf()
        )
    }
    override fun scaleBy(by: Double) {}
}