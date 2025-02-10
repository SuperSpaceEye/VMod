package net.spaceeye.vmod.utils.vs

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntityManager
import net.spaceeye.vmod.vEntityManaging.VSJointUser
import net.spaceeye.vmod.vsStuff.VSJointsTracker
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.shipObjectWorld

data class TraversedData(
    val traversedShipIds: MutableSet<ShipId>,
    val traversedVEntityIds: MutableSet<Int>,
    val traversedVSJointIds: MutableSet<Int>
)

fun traverseGetAllTouchingShips(level: ServerLevel, shipId: ShipId, blacklist: Set<ShipId> = setOf(), withJointInfo: Boolean = false): Set<ShipId> {
    val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values.toSet()

    val stack = mutableListOf<ShipId>(shipId)
    val traversedShips = mutableSetOf<ShipId>()
    traversedShips.addAll(dimensionIds)
    traversedShips.addAll(blacklist)

    while (stack.isNotEmpty()) {
        val shipId = stack.removeLast()
        if (traversedShips.contains(shipId)) {continue}

        val data = traverseGetConnectedShips(shipId, traversedShips, withJointInfo)
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

//TODO i should redo this
fun traverseGetConnectedShips(shipId: ShipId, blacklist: Set<ShipId> = setOf(), withJointInfo: Boolean = false): TraversedData {
    val instance = VEntityManager.getInstance()
    val dimensionIds = ServerLevelHolder.server!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

    val stack = mutableListOf<ShipId>(shipId)
    val traversedShips = mutableSetOf<ShipId>()
    traversedShips.addAll(dimensionIds)
    traversedShips.addAll(blacklist)

    val traversedVEntities = mutableSetOf<Int>()
    val traversedVSJoints = mutableSetOf<Int>()

    while (stack.isNotEmpty()) {
        val shipId = stack.removeLast()
        if (traversedShips.contains(shipId)) {continue}
        traversedShips.add(shipId)

        val vsIdsOfVEntities = mutableSetOf<VSJointId>()
        instance.getAllVEntitiesIdOfId(shipId).forEach {
            if (traversedVEntities.contains(it)) { return@forEach }
            traversedVEntities.add(it)
            val constraint = instance.getVEntity(it) ?: return@forEach
            stack.addAll(constraint.attachedToShips(dimensionIds).filter { !traversedShips.contains(it) })
            if (constraint is VSJointUser) {vsIdsOfVEntities.addAll(constraint.getVSIds())}
        }

        if (!withJointInfo) {
            val connectedShips = VSJointsTracker.getConnected(shipId)
            stack.addAll(connectedShips.subtract(traversedShips))
        } else {
            val constraintIds = VSJointsTracker.getIdsOfShip(shipId).filter { !traversedVSJoints.contains(it) && !vsIdsOfVEntities.contains(it) }
            VSJointsTracker.getVSJoints(constraintIds).forEach {
                val constraint = it.second ?: return@forEach
                if (!traversedShips.contains(constraint.shipId0)) constraint.shipId0?.let{stack.add(it)}
                if (!traversedShips.contains(constraint.shipId1)) constraint.shipId1?.let{stack.add(it)}
            }
            traversedVSJoints.addAll(constraintIds)
        }
    }
    traversedShips.removeAll(dimensionIds.toSet())
    traversedShips.removeAll(blacklist)

    return TraversedData(traversedShips, traversedVEntities, traversedVSJoints)
}