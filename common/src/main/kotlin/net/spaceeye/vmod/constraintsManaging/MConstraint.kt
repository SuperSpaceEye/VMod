package net.spaceeye.vmod.constraintsManaging

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointId

interface Tickable {
    fun tick(server: MinecraftServer, unregister: () -> Unit)
}

interface MConstraint {
    var mID: ManagedConstraintId

    fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which constraint is "attached" to the ship/world
    // is needed for strip tool, moving constraints on ship splitting
    fun getAttachmentPositions(shipId: Long = -1): List<BlockPos>
    fun getAttachmentPoints(shipId: Long = -1): List<Vector3d>

    // is called on ship splitting
    fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint?

    fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)
    fun getVSIds(): Set<VSJointId>

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal fun onMakeMConstraint(level: ServerLevel): Boolean
    @Internal fun onDeleteMConstraint(level: ServerLevel)
}