package net.spaceeye.vmod.constraintsManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import org.valkyrienskies.core.api.ships.properties.ShipId

class Strippable: MConstraintExtension {
    override fun onInit(obj: ExtendableMConstraint) {}

    override fun onAfterCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableMConstraint) {
        new.addExtension(Strippable())
    }

    override fun onSerialize(): CompoundTag? {
        return CompoundTag()
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        return true
    }

    override fun onMakeMConstraint(level: ServerLevel) {}
    override fun onDeleteMConstraint(level: ServerLevel) {}
}