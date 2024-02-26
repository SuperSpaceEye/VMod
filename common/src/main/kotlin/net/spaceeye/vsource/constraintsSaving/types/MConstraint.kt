package net.spaceeye.vsource.constraintsSaving.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsSaving.ManagedConstraintId
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.properties.ShipId

interface MConstraint {
    var mID: ManagedConstraintId
    val shipId0: ShipId
    val shipId1: ShipId

    val typeName: String

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal fun addVSConstraintsToLevel(level: ServerLevel): Boolean
    @Internal fun removeVSConstraints(level: ServerLevel)
}