package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import org.valkyrienskies.core.api.ships.properties.ShipId

open class Strippable: VEntityExtension {
    override fun onInit(obj: ExtendableVEntity) {}

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) {
        new.addExtension(Strippable())
    }

    override fun onSerialize(): CompoundTag? {
        return CompoundTag()
    }

    override fun onDeserialize(tag: CompoundTag): Boolean {
        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {}
    override fun onDeleteVEntity(level: ServerLevel) {}
}