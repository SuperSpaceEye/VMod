package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.rendering.types.BaseRenderer
import net.spaceeye.vsource.rendering.types.RopeRenderer
import net.spaceeye.vsource.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId

interface Tickable {
    fun tick(server: MinecraftServer, unregister: () -> Unit)
}

interface MConstraint {
    var mID: ManagedConstraintId
    val typeName: String
    // SHOULD BE SET TO -1.
    // DO NOT USE IT ANYWHERE. JUST IMPLEMENT AS SIMPLE VAR
    var saveCounter: Int

    fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which constraint is "attached" to the ship/world
    // is needed for strip tool, moving constraints on ship splitting
    fun getAttachmentPoints(): List<BlockPos>

    // is called on ship splitting
    fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal fun onMakeMConstraint(level: ServerLevel): Boolean
    @Internal fun onDeleteMConstraint(level: ServerLevel)
}

fun updatePositions(
    newShipId: ShipId,
    previous: BlockPos,
    new: BlockPos,

    attachmentPoints: List<BlockPos>,

    shipIds: MutableList<ShipId>,
    localPoints: MutableList<List<Vector3dc>>
) {
    for (i in attachmentPoints.indices) {
        val apoint = attachmentPoints[i]
        if (previous != apoint) {continue}
        shipIds[i] = newShipId
        localPoints[i] = localPoints[i].mapIndexed {
            i, it ->
            (Vector3d(it) - Vector3d(apoint) + Vector3d(new)).toJomlVector3d()
        }
    }
}

fun updateRenderer(
    localPos0: Vector3dc,
    localPos1: Vector3dc,
    shipId0: ShipId,
    shipId1: ShipId,
    mID: ManagedConstraintId
): BaseRenderer? {
    val renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID.id) ?: return null
    SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID.id)

    when (renderer) {
        is RopeRenderer -> {
            renderer.point1 = Vector3d(localPos0)
            renderer.point2 = Vector3d(localPos1)
            SynchronisedRenderingData.serverSynchronisedData.addRenderer(shipId0, shipId1, mID.id, renderer)
        }

        is A2BRenderer -> {
            renderer.point1 = Vector3d(localPos0)
            renderer.point2 = Vector3d(localPos1)
            SynchronisedRenderingData.serverSynchronisedData.addRenderer(shipId0, shipId1, mID.id, renderer)
        }
    }
    return renderer
}