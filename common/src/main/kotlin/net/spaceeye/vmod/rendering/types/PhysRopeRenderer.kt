package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderSetups
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.drawPolygonTube
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.makePolygonPoints
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.physics.VSCapsuleCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class PhysRopeRenderer(): BaseRenderer(), ReflectableObject {
    class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var shipId1: Long by get(i++, -1L)
        var shipId2: Long by get(i++, -1)

        var point1: Vector3d by get(i++, Vector3d())
        var point2: Vector3d by get(i++, Vector3d())

        var up1: Vector3d by get(i++, Vector3d())
        var up2: Vector3d by get(i++, Vector3d())

        var right1: Vector3d by get(i++, Vector3d())
        var right2: Vector3d by get(i++, Vector3d())

        var color: Color by get(i++, Color(0))
        var sides: Int by get(i++, 8, true) { ClientLimits.instance.physRopeSides.get(it) }
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }

        var shipIds: LongArray by get(i++, longArrayOf())

        var texture: ResourceLocation by get(i++, RenderingUtils.ropeTexture)

        var lengthUVStart: Float by get(i++, 0f)
        var lengthUVIncMultiplier: Float by get(i++, 1f)
        var widthUVStart: Float by get(i++, 0f)
        var widthUVMultiplier: Float by get(i++, 1f)
    }
    var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    //Same order as data
    constructor(vararg items: Any?): this() { data.setFromVararg(items) }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    private var highlightTick = 0L
    override fun highlightUntilRenderingTicks(until: Long) {
        if (until > highlightTick) highlightTick = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) = with(data) {
        val level = Minecraft.getInstance().level!!
        val sides = sides
        val fullbright = fullbright

        val entities = shipIds.map { (level.shipObjectWorld as ShipObjectClientWorld).physicsEntities[it] }.filterNotNull()
        if (entities.isEmpty()) { return }

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        val color = if (timestamp < highlightTimestamp || renderingTick < highlightTick) Color(255, 0, 0, 255) else color

        RenderSystem.setShaderTexture(0, texture)
        vBuffer.begin(VertexFormat.Mode.QUADS, RenderSetups.setupFullRendering())

        poseStack.pushPose()

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

        var ppos = (if (ship1 == null) point1 else posShipToWorldRender(ship1, point1))
        var cpos = Vector3d(entities[0].renderTransform.positionInWorld)

        var up    = ship1?.shipToWorld?.transformDirection(up1   .toJomlVector3d())?.let { Vector3d(it) } ?: up1
        var right = ship1?.shipToWorld?.transformDirection(right1.toJomlVector3d())?.let { Vector3d(it) } ?: right1

        var shape = entities[0].collisionShapeData as VSCapsuleCollisionShapeData
        var rPoints: List<Vector3d>
        var lPoints = makePolygonPoints(sides, shape.radius, up, right, ppos)

        //TODO make widthUV more configurable
        val widthUV = (shape.radius * 2f * widthUVMultiplier).toFloat()
        var leftUV = lengthUVStart

        for (entity in entities) {
            shape = entity.collisionShapeData as VSCapsuleCollisionShapeData

            val capsuleDir = -Vector3d(shape.length, 0, 0) * 0.5
            cpos = posShipToWorldRender(null, capsuleDir, entity.renderTransform)

            up = getUpFromQuat(entity.renderTransform.shipToWorldRotation)
            rPoints = makePoints(cpos, ppos, cpos, up, shape.radius)
            val rightUV = leftUV + shape.length.toFloat() * lengthUVIncMultiplier

            val leftLight  = if (fullbright) LightTexture.FULL_BRIGHT else ppos.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
            val rightLight = if (fullbright) LightTexture.FULL_BRIGHT else cpos.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
            drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, leftLight, rightLight, leftUV, rightUV, widthUVStart, widthUV, lPoints, rPoints)

            leftUV = rightUV
            lPoints = rPoints
            ppos = cpos
        }

        cpos = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        up    = ship2?.shipToWorld?.transformDirection(up2   .toJomlVector3d())?.let { Vector3d(it) } ?: up2
        right = ship2?.shipToWorld?.transformDirection(right2.toJomlVector3d())?.let { Vector3d(it) } ?: right2

        rPoints = makePolygonPoints(sides, shape.radius, up, right, cpos)
        val rightUV = leftUV + shape.length.toFloat()

        val leftLight  = if (fullbright) LightTexture.FULL_BRIGHT else ppos.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
        val rightLight = if (fullbright) LightTexture.FULL_BRIGHT else cpos.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
        drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, leftLight, rightLight, leftUV, rightUV, 0f, widthUV, lPoints, rPoints)

        tesselator.end()
        poseStack.popPose()

        RenderSetups.clearFullRendering()
    }

    private fun makePoints(cpos: Vector3d, ppos: Vector3d, posToUse: Vector3d, up: Vector3d, width: Double) = with(data) { return@with makePolygonPoints(sides, width, up, (cpos - ppos).snormalize().scross(up), posToUse) }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = with(data) {
        return PhysRopeRenderer(
            oldToNew[shipId1]?.id ?: -1,
            oldToNew[shipId2]?.id ?: -1,
            centerPositions[shipId1]?.let { (old, new) -> updatePosition(point1, old, new) } ?: point1,
            centerPositions[shipId2]?.let { (old, new) -> updatePosition(point2, old, new) } ?: point2,
            up1.copy(), up2.copy(), right1.copy(), right2.copy(),
            color, sides, fullbright, longArrayOf(), texture
        )
    }
    override fun scaleBy(by: Double) {}
}