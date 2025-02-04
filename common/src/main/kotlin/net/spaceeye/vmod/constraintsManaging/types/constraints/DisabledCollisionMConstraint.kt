package net.spaceeye.vmod.constraintsManaging.types.constraints

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.constraintsManaging.util.MCAutoSerializable
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointId
import net.spaceeye.vmod.reflectable.ReflectableItem.get

class DisabledCollisionMConstraint(): ExtendableMConstraint(), MCAutoSerializable {
    @JsonIgnore private var i = 0

    var shipId1: Long by get(i++, -1L)
    var shipId2: Long by get(i++, -1L)

    constructor(shipId1: ShipId, shipId2: ShipId): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
    }

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(shipId1)
        val ship2Exists = allShips.contains(shipId2)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(shipId1))
                || (ship2Exists && dimensionIds.contains(shipId2))
    }

    override fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(shipId1)) {toReturn.add(shipId1)}
        if (!dimensionIds.contains(shipId2)) {toReturn.add(shipId2)}

        return toReturn
    }

    override fun iGetAttachmentPositions(shipId: Long): List<BlockPos> = emptyList()
    override fun iGetAttachmentPoints(shipId: Long): List<Vector3d> = emptyList()
    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun iGetVSIds(): Set<VSJointId> = emptySet()
    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? { return DisabledCollisionMConstraint(mapped[shipId1] ?: return null, mapped[shipId2] ?: return null) }

    private var beingRemoved = false
    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        return level.disableCollisionBetween(shipId1, shipId2) {
            if (!beingRemoved) {
                beingRemoved = true
                level.removeManagedConstraint(this)
            }
        }
    }
    override fun iOnDeleteMConstraint(level: ServerLevel) {
        beingRemoved = true
        level.enableCollisionBetween(shipId1, shipId2)
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        level.makeManagedConstraint(DisabledCollisionMConstraint(shipId1, newShipId)) {}
        level.makeManagedConstraint(DisabledCollisionMConstraint(shipId2, newShipId)) {}
    }
}