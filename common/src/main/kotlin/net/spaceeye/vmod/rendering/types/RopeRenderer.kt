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
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderTypes
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

class RopeRenderer(): BaseRenderer(), ReflectableObject {
    private class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var shipId1: Long by get(i++, -1L)
        var shipId2: Long by get(i++, -1L)

        var point1: Vector3d by get(i++, Vector3d())
        var point2: Vector3d by get(i++, Vector3d())

        var length: Double by get(i++, 0.0)

        var width: Double by get(i++, .2, true) { ClientLimits.instance.ropeRendererWidth.get(it) }
        var segments: Int by get(i++, 16, true) { ClientLimits.instance.ropeRendererSegments.get(it) }
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    constructor(
        shipId1: Long,
        shipId2: Long,
        point1: Vector3d,
        point2: Vector3d,
        length: Double,
        width: Double,
        segments: Int,
        fullbright: Boolean,
    ): this() { with(data) {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.point1 = point1
        this.point2 = point2
        this.length = length
        this.width = width
        this.segments = segments
        this.fullbright = fullbright
    } }

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) = with(data) {
        val level = Minecraft.getInstance().level!!

        val ship1 = if (shipId1 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId1) ?: return } else null
        val ship2 = if (shipId2 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId2) ?: return } else null

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 0f, 0f, 1f)
        }

        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)
        vBuffer.begin(VertexFormat.Mode.QUADS, RenderTypes.setupFullRendering())

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.drawRope(
            vBuffer, matrix,
            255, 255, 255, 255,
            width, segments, length,
            tpos1, tpos2,
            if (fullbright) { { LightTexture.FULL_BRIGHT} } else { pos -> (pos + cameraPos).toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) } }
        )

        tesselator.end()
        poseStack.popPose()

        RenderTypes.clearFullRendering()

        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = with(data) {
        val spoint1 = if (shipId1 != -1L) {centerPositions[shipId1]!!.let { (old, new) -> updatePosition(point1, old, new)} } else {Vector3d(point1)}
        val spoint2 = if (shipId2 != -1L) {centerPositions[shipId2]!!.let { (old, new) -> updatePosition(point2, old, new)} } else {Vector3d(point2)}

        val newId1 = if (shipId1 != -1L) {oldToNew[shipId1]!!.id} else {-1}
        val newId2 = if (shipId2 != -1L) {oldToNew[shipId2]!!.id} else {-1}

        return RopeRenderer(newId1, newId2, spoint1, spoint2, length, width, segments, fullbright)
    }

    override fun scaleBy(by: Double) = with(data) {
        width *= by
        length *= by
    }
}