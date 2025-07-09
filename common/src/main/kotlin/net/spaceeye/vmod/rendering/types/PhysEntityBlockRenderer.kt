package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderingStuff
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toFloat
import java.awt.Color

class PhysEntityBlockRenderer(): BlockRenderer(), ReflectableObject {
    private class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var shipId: Long by get(i++, -1L)
        var stateTag: CompoundTag by get(i++, CompoundTag())
        var color: Color by get(i++, Color(255, 255, 255))
        var fullbright: Boolean by get(i++, false, true) { ClientLimits.instance.lightingMode.get(it) }
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    constructor(shipId: ShipId, state: BlockState, color: Color, fullbright: Boolean): this() { with(data) {
        this.shipId = shipId
        this.stateTag = NbtUtils.writeBlockState(state)
        this.color = color
        this.fullbright = fullbright
    } }

    private var state: BlockState? = null

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource, timestamp: Long) = with(data) {
        val level = Minecraft.getInstance().level!!
        val entity = (level.shipObjectWorld as ShipObjectClientWorld).physicsEntities[shipId] ?: return
        val lookup = BuiltInRegistries.BLOCK.asLookup()
        state = stateTag.let { try { NbtUtils.readBlockState(lookup, it) } catch (e: Exception) { null } } ?: return@with

        val pos = entity.renderTransform.position
        val rot = entity.renderTransform.rotation

        val light = if (fullbright) LightTexture.FULL_BRIGHT else Vector3d(pos).toBlockPos().let { LightTexture.pack(level.getBrightness(LightLayer.BLOCK, it), level.getBrightness(LightLayer.SKY, it)) }
        val combinedOverlayIn = OverlayTexture.NO_OVERLAY

        poseStack.pushPose()

        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(pos.x(), pos.y(), pos.z())
        poseStack.mulPose(rot.toFloat())
        poseStack.translate(-0.5, -0.5, -0.5)

        RenderingStuff.renderSingleBlock(state!!, poseStack, buffer, light, combinedOverlayIn, color)

        poseStack.popPose()
    }

    override fun copy(
        oldToNew: Map<ShipId, Ship>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>
    ): BaseRenderer? {
        TODO("Not yet implemented")
    }

    override fun scaleBy(by: Double) {
        TODO("Not yet implemented")
    }
}