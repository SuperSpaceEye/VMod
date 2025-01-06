package net.spaceeye.vmod.utils.vs

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.constraintsManaging.VSJointsTracker
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.shipObjectWorld

data class TraversedData(
    val traversedShipIds: MutableSet<ShipId>,
    val traversedMConstraintIds: MutableSet<Int>,
    val traversedVSJointIds: MutableSet<Int>
)

fun traverseGetAllTouchingShips(level: ServerLevel, shipId: ShipId, blacklist: Set<ShipId> = setOf()): Set<ShipId> {
    val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values.toSet()

    val stack = mutableListOf<ShipId>(shipId)
    val traversedShips = mutableSetOf<ShipId>()
    traversedShips.addAll(dimensionIds)
    traversedShips.addAll(blacklist)

    while (stack.isNotEmpty()) {
        val shipId = stack.removeLast()
        if (traversedShips.contains(shipId)) {continue}

        val data = traverseGetConnectedShips(shipId, traversedShips)
        data.traversedShipIds.remove(shipId)
        traversedShips.add(shipId)

        stack.addAll(data.traversedShipIds)

        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: continue
        stack.addAll(level
            .getShipsIntersecting(ship.worldAABB)
            .map { it.id }
            .filter { !traversedShips.contains(it) }
        )
    }
    traversedShips.removeAll(dimensionIds)
    traversedShips.removeAll(blacklist)

    return traversedShips
}

fun traverseGetConnectedShips(shipId: ShipId, blacklist: Set<ShipId> = setOf()): TraversedData {
    val instance = ConstraintManager.getInstance()
    val dimensionIds = ServerLevelHolder.server!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

    val stack = mutableListOf<ShipId>(shipId)
    val traversedShips = mutableSetOf<ShipId>()
    traversedShips.addAll(dimensionIds)
    traversedShips.addAll(blacklist)

    val traversedMConstraints = mutableSetOf<Int>()
    val traversedVSJoints = mutableSetOf<Int>()

    while (stack.isNotEmpty()) {
        val shipId = stack.removeLast()
        if (traversedShips.contains(shipId)) {continue}
        traversedShips.add(shipId)

        val vsIdsOfMConstraints = mutableSetOf<VSJointId>()
        instance.getAllConstraintsIdOfId(shipId).forEach {
            if (traversedMConstraints.contains(it)) { return@forEach }
            traversedMConstraints.add(it)
            val constraint = instance.getManagedConstraint(it) ?: return@forEach
            stack.addAll(constraint.attachedToShips(dimensionIds).filter { !traversedShips.contains(it) })
            vsIdsOfMConstraints.addAll(constraint.getVSIds())
        }

        val constraintIds = VSJointsTracker.getIdsOfShip(shipId).filter { !traversedVSJoints.contains(it) && !vsIdsOfMConstraints.contains(it) }
        VSJointsTracker.getVSJoints(constraintIds).forEach {
            val constraint = it.second ?: return@forEach
            if (!traversedShips.contains(constraint.shipId0)) stack.add(constraint.shipId0!!) //TODO assumes shipid is always not null which is wrong
            if (!traversedShips.contains(constraint.shipId1)) stack.add(constraint.shipId1!!) //TODO assumes shipid is always not null which is wrong
        }
        traversedVSJoints.addAll(constraintIds)
    }
    traversedShips.removeAll(dimensionIds.toSet())
    traversedShips.removeAll(blacklist)

    return TraversedData(traversedShips, traversedMConstraints, traversedVSJoints)
}