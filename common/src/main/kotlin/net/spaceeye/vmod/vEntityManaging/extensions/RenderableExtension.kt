package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.reflectable.tDeserialize
import net.spaceeye.vmod.reflectable.tSerialize
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.RenderingTypes
import net.spaceeye.vmod.rendering.RenderingTypes.getType
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

open class RenderableExtension(): VEntityExtension {
    lateinit var renderer: BaseRenderer
    var rID = -1
        protected set
    protected lateinit var obj: ExtendableVEntity

    constructor(renderer: BaseRenderer): this() {
        this.renderer = renderer
    }

    override fun onInit(obj: ExtendableVEntity) {
        this.obj = obj
    }

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) {
        val newRenderer = renderer.copy(mapped.map { (k, v) -> Pair(k, level.shipObjectWorld.allShips.getById(v)!!) }.toMap(), centerPositions) ?: return
        new.addExtension(RenderableExtension(newRenderer))
    }

    override fun onBeforeOnScaleByVEntity(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        renderer.scaleBy(scaleBy)
        RenderingData.server.setUpdated(rID, renderer)
    }

    override fun onSerialize(): CompoundTag? {
        val tag = CompoundTag()
        try {
            tag.putString("rendererType", renderer.getType())
            tag.put("renderer", (renderer as ReflectableObject).tSerialize())
        } catch (e: Exception) { ELOG("FAILED TO SERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}"); return null
        } catch (e: Error) { ELOG("FAILED TO SERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}"); return null }
        return tag
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        if (!tag.contains("renderer")) {return false}
        try {
            val type = tag.getString("rendererType")
            renderer = RenderingTypes.strTypeToSupplier(type).get()
            (renderer as ReflectableObject).tDeserialize(tag.getCompound("renderer"))
        } catch (e: Exception) { ELOG("FAILED TO DESERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}"); return false
        } catch (e: Error) { ELOG("FAILED TO DESERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}"); return false}

        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {
        val ids = obj.attachedToShips(listOf())
        RenderingData.server.removeRenderer(rID)
        rID = RenderingData.server.addRenderer(ids, renderer)
    }

    override fun onDeleteVEntity(level: ServerLevel) {
        RenderingData.server.removeRenderer(rID)
    }
}