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
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.drawTubeRope
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import kotlin.let

class TubeRopeRenderer(): BaseRenderer(), ReflectableObject {
    private class Data(): AutoSerializable {
        @JsonIgnore private var i = 0

        var shipId1: Long by get(i++, -1L)
        var shipId2: Long by get(i++, -1L)

        var point1: Vector3d by get(i++, Vector3d())
        var point2: Vector3d by get(i++, Vector3d())

        var up1: Vector3d by get(i++, Vector3d())
        var up2: Vector3d by get(i++, Vector3d())

        var right1: Vector3d by get(i++, Vector3d())
        var right2: Vector3d by get(i++, Vector3d())

        var length: Double by get(i++, 0.0)

        var color: Color by get(i++, Color.WHITE)

        var width: Double by get(i++, .2, true) { ClientLimits.instance.tubeRopeRendererWidth.get(it) }
        var sides: Int by get(i++, 4, true) { ClientLimits.instance.tubeRopeRendererSides.get(it) }
        var segments: Int by get(i++, 16) { ClientLimits.instance.tubeRopeRendererSegments.get(it) }
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }
        var lerpBetweenRotations: Boolean by get(i++, false)
        var useDefinedUpRight: Boolean by get(i++, false)

        var texture: ResourceLocation by get(i++, RenderingUtils.ropeTexture)

        var lengthUVStart: Float by get(i++, 0f)
        var lengthUVIncMultiplier: Float by get(i++, 1f)
        var widthUVStart: Float by get(i++, 0f)
        var widthUVMultiplier: Float by get(i++, 1f)
    }
    private var data = Data()
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

    override fun renderData(
        poseStack: PoseStack,
        camera: Camera,
        timestamp: Long
    ) = with(data) {
        val level = Minecraft.getInstance().level!!

        val ship1 = if (shipId1 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId1) ?: return } else null
        val ship2 = if (shipId2 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId2) ?: return } else null

        val rPos1 = if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)
        val rPos2 = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        val up1    = ship1?.shipToWorld?.transformDirection(up1   .toJomlVector3d())?.let { Vector3d(it) } ?: up1
        val right1 = ship1?.shipToWorld?.transformDirection(right1.toJomlVector3d())?.let { Vector3d(it) } ?: right1

        val up2    = ship2?.shipToWorld?.transformDirection(up2   .toJomlVector3d())?.let { Vector3d(it) } ?: up2
        val right2 = ship2?.shipToWorld?.transformDirection(right2.toJomlVector3d())?.let { Vector3d(it) } ?: right2

        val color = if (timestamp < highlightTimestamp || renderingTick < highlightTick) Color(255, 0, 0, 255) else color

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        vBuffer.begin(VertexFormat.Mode.QUADS, RenderSetups.setupFullRendering())
        RenderSystem.setShaderTexture(0, texture)

        poseStack.pushPose()

        val cameraPos = -Vector3d(camera.position)
        poseStack.translate(cameraPos.x, cameraPos.y, cameraPos.z)

        val matrix = poseStack.last().pose()

        drawTubeRope(
            vBuffer, matrix, color.red, color.green, color.blue, color.alpha, width, segments, sides,
            length, rPos1, rPos2, up1, right1, up2, right2, lerpBetweenRotations, useDefinedUpRight,
            lengthUVStart, lengthUVIncMultiplier, widthUVStart, widthUVMultiplier,
            if (fullbright) { _ -> LightTexture.FULL_BRIGHT } else { pos ->
                pos.toBlockPos().let {
                    LightTexture.pack(
                        level.getBrightness(LightLayer.BLOCK, it),
                        level.getBrightness(LightLayer.SKY, it)
                    )
                }
            })

        poseStack.popPose()
        tesselator.end()
        RenderSetups.clearFullRendering()
    }

    override fun copy(
        oldToNew: Map<ShipId, Ship>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>
    ): BaseRenderer? = with(data) {
        return TubeRopeRenderer(
            oldToNew[shipId1]?.id ?: -1,
            oldToNew[shipId2]?.id ?: -1,
            centerPositions[shipId1]?.let { (old, new) -> updatePosition(point1, old, new) } ?: point1,
            centerPositions[shipId2]?.let { (old, new) -> updatePosition(point2, old, new) } ?: point2,
            up1.copy(), up2.copy(), right1.copy(), right2.copy(),
            length, color, width, sides, segments, fullbright, lerpBetweenRotations, useDefinedUpRight, texture,
            lengthUVStart, lengthUVIncMultiplier, widthUVStart, widthUVMultiplier
        )
    }

    override fun scaleBy(by: Double) = with(data) {
        width *= by
    }
}