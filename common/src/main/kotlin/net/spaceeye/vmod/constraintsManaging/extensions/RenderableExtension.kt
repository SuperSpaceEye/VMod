package net.spaceeye.vmod.constraintsManaging.extensions

import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import net.spaceeye.vmod.rendering.RenderingTypes
import net.spaceeye.vmod.rendering.RenderingTypes.getType
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

class RenderableExtension(): MConstraintExtension {
    private lateinit var renderer: BaseRenderer
    private var rID = -1
    private lateinit var obj: ExtendableMConstraint

    constructor(renderer: BaseRenderer): this() {
        this.renderer = renderer
    }

    fun getRID(): Int = rID

    override fun onInit(obj: ExtendableMConstraint) {
        this.obj = obj
    }

    override fun onAfterCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableMConstraint) {
        val newRenderer = renderer.copy(mapped.map {(k, v) -> Pair(k, level.shipObjectWorld.allShips.getById(v)!!) }.toMap()) ?: return
        new.addExtension(RenderableExtension(newRenderer))
    }

    override fun onBeforeOnScaleByMConstraint(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        renderer.scaleBy(scaleBy)
        ServerRenderingData.setUpdated(rID, renderer)
    }

    override fun onSerialize(): CompoundTag? {
        val tag = CompoundTag()
        try {
            tag.putString("rendererType", renderer.getType())
            tag.putByteArray("renderer", renderer.serialize().accessByteBufWithCorrectSize())
        } catch (e: Exception) { ELOG("FAILED TO SERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}"); return null
        } catch (e: Error) { ELOG("FAILED TO SERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}"); return null }
        return tag
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        if (!tag.contains("renderer")) {return false}
        try {
            val type = tag.getString("rendererType")
            renderer = RenderingTypes.strTypeToSupplier(type).get()
            renderer.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("renderer"))))
        } catch (e: Exception) { ELOG("FAILED TO DESERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}"); return false
        } catch (e: Error) { ELOG("FAILED TO DESERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}"); return false}

        return true
    }

    override fun onMakeMConstraint(level: ServerLevel) {
        val ids = obj.attachedToShips(listOf())
        ServerRenderingData.removeRenderer(rID)
        rID = ServerRenderingData.addRenderer(ids[0], if (ids.size > 1) {ids[1]} else {ids[0]}, renderer)
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        ServerRenderingData.removeRenderer(rID)
    }
}