package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.textures.GIFManager
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

open class A2BRendererAnimated(): BaseRenderer(), ReflectableObject {
    private class Data: AutoSerializable {
        @JsonIgnore private var i = 0

        var shipId1: Long by get(i++, -1L)
        var shipId2: Long by get(i++, -1L)

        var point1: Vector3d by get(i++, Vector3d())
        var point2: Vector3d by get(i++, Vector3d())

        var color: Color by get(i++, Color(0))

        var width: Double by get(i++, .2, true) { ClientLimits.instance.lineRendererWidth.get(it) }
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }

        var texture: ResourceLocation by get(i++, ResourceLocation(MOD_ID, "textures/gif/test_gif3.gif"))
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    var gifRef: GIFManager.TextureReference? = null

    constructor(
        shipId1: Long,
        shipId2: Long,
        point1: Vector3d,
        point2: Vector3d,
        color: Color,
        width: Double,
        fullbright: Boolean,
        texture: ResourceLocation
    ): this() { with(data) {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.point1 = point1
        this.point2 = point2
        this.color = color
        this.width = width
        this.fullbright = fullbright
        this.texture = texture
    } }

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

        val gifRef = gifRef ?: let { gifRef = GIFManager.getTextureFromLocation(texture); gifRef!! }

        val ship1 = if (shipId1 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId1) ?: return } else null
        val ship2 = if (shipId2 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId2) ?: return } else null

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)
        val center = (rpoint1 + rpoint2) / 2

        val color = if (timestamp < highlightTimestamp || renderingTick < highlightTick) Color(255, 0, 0, 255) else color

        val light = if (fullbright) LightTexture.FULL_BRIGHT else center.toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val pos1 = rpoint1 - cameraPos
        val pos2 = rpoint2 - cameraPos

        val wdir = pos2 - pos1
        // perpendicular direction
        val pdir = (-pos1 - wdir * ((-pos1).dot(wdir) / wdir.dot(wdir))).snormalize()

        val up = pdir.cross(wdir.normalize())

        val lu =  up * width + pos1
        val ld = -up * width + pos1
        val rd = -up * width + pos2
        val ru =  up * width + pos2

        gifRef.gif.advanceTime(Minecraft.getInstance().frameTime * 50)
        gifRef.gif.draw(poseStack, lu, ld, rd, ru)
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = with(data) {
        val spoint1 = if (shipId1 != -1L) {centerPositions[shipId1]!!.let { (old, new) -> updatePosition(point1, old, new)} } else {Vector3d(point1)}
        val spoint2 = if (shipId2 != -1L) {centerPositions[shipId2]!!.let { (old, new) -> updatePosition(point2, old, new)} } else {Vector3d(point2)}

        val newId1 = if (shipId1 != -1L) {oldToNew[shipId1]!!.id} else {-1}
        val newId2 = if (shipId2 != -1L) {oldToNew[shipId2]!!.id} else {-1}

        return A2BRendererAnimated(newId1, newId2, spoint1, spoint2, color, width, fullbright, texture)
    }

    override fun scaleBy(by: Double) = with(data) {
        width *= by
    }
}