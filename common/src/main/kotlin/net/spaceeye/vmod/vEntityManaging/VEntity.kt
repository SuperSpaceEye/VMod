package net.spaceeye.vmod.vEntityManaging

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.compat.vsBackwardsCompat.*

interface Tickable {
    fun tick(server: MinecraftServer, unregister: () -> Unit)
}

interface VSJointUser {
    fun getVSIds(): Set<VSJointId>
}

//VMod Entity -> VEntity
interface VEntity {
    var mID: VEntityId
    var dimensionId: String?

    fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which VEntity is "attached" to the ship/world
    // is needed for strip tool, moving VEntities on ship splitting
    /**
     * By default (-1) should return all attachment positions
     * If given shipId, should only return positions belonging to that shipId
     */
    fun getAttachmentPoints(shipId: Long = -1): List<Vector3d>

    // is called on ship splitting
    fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    fun copyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity?

    fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity?

    @Internal fun onMakeVEntity(level: ServerLevel): Boolean
    @Internal fun onDeleteVEntity(level: ServerLevel)
}