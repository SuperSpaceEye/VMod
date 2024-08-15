package net.spaceeye.vmod.constraintsManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import org.valkyrienskies.core.api.ships.properties.ShipId

open class NonStrippable: MConstraintExtension {
    override fun onInit(obj: ExtendableMConstraint) {}

    override fun onAfterCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableMConstraint) {
        new.addExtension(NonStrippable())
    }

    override fun onSerialize(): CompoundTag? {
        return CompoundTag()
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        return true
    }

    override fun onMakeMConstraint(level: ServerLevel) {}
    override fun onDeleteMConstraint(level: ServerLevel) {}
    override val typeName: String get() = "NonStrippable"
}