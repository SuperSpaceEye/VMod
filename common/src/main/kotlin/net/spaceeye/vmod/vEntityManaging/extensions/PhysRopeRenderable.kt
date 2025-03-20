package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.vEntityManaging.types.constraints.PhysRopeConstraint
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

class PhysRopeRenderable(): RenderableExtension() {
    constructor(renderer: PhysRopeRenderer): this() {
        this.renderer = renderer
    }

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableVEntity) {
        val newRenderer = renderer.copy(mapped.map {(k, v) -> Pair(k, level.shipObjectWorld.allShips.getById(v)!!) }.toMap()) ?: return
        new.addExtension(PhysRopeRenderable(newRenderer as PhysRopeRenderer))
    }

    override fun onMakeVEntity(level: ServerLevel) {
        val obj = obj as PhysRopeConstraint
        val renderer = this.renderer as PhysRopeRenderer

        renderer.shipIds = obj.entities.map { it.id }.toLongArray()
        this.renderer = renderer

        super.onMakeVEntity(level)
    }
}