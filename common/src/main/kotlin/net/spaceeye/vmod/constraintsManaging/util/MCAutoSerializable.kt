package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.networking.TagAutoSerializable
import org.valkyrienskies.core.api.ships.properties.ShipId

/**
 * Adds [TagAutoSerializable] interface and implements [iNbtSerialize] and [iNbtDeserialize] for [ExtendableMConstraint]
 */
interface MCAutoSerializable: MConstraint, TagAutoSerializable, ExtendableMConstraintIMethods {
    override fun iNbtSerialize(): CompoundTag? = tSerialize()
    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? = tDeserialize(tag).let { this }
}