package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.properties.ShipId

interface Tickable {
    fun tick(server: MinecraftServer, unregister: () -> Unit)
}

interface MConstraint {
    var mID: ManagedConstraintId
    val shipId0: ShipId
    val shipId1: ShipId

    val typeName: String

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal fun onMakeMConstraint(level: ServerLevel): Boolean
    @Internal fun onDeleteMConstraint(level: ServerLevel)
}