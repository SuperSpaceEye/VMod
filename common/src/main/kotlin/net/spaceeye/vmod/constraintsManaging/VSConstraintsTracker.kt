package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.mixin.ShipObjectWorldAccessor
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld

object VSConstraintsTracker: ServerClosable() {
    val idToShips = mutableMapOf<Int, Pair<ShipId, ShipId>>()
    val shipToIds: Map<Long, Set<Int>> get() = (ServerLevelHolder.shipObjectWorld!! as ShipObjectWorldAccessor).shipIdToConstraints

    fun addNewConstraint(constraint: VSConstraint, constraintId: VSConstraintId) {
        idToShips[constraintId] = Pair(constraint.shipId0, constraint.shipId1)
    }

    fun removeConstraint(constraint: VSConstraint, constraintId: VSConstraintId) {
        idToShips.remove(constraintId)
    }

    fun updateConstraint(constraint: VSConstraint, constraintId: VSConstraintId) {
        val shipIds = idToShips[constraintId] ?: run { ELOG("THIS SHOULD NEVER HAPPEN. IN VSConstraintsKeeper.updateConstraint idToShip with ${constraintId} IS null.") ; null } ?: return
        if (shipIds.first == constraint.shipId0 && shipIds.second == constraint.shipId1) { return }

        removeConstraint(constraint, constraintId)
        addNewConstraint(constraint, constraintId)
    }

    fun getVSConstraints(ids: List<VSConstraintId>): List<Pair<VSConstraintId, VSConstraint?>> {
        val acc = ServerLevelHolder.server!!.shipObjectWorld as ShipObjectWorldAccessor
        return ids.map { Pair(it, acc.constraints[it]) }
    }

    fun getIdsOfShip(shipId: ShipId): Set<Int> {
        return shipToIds.getOrDefault(shipId, setOf())
    }

    override fun close() {
        idToShips.clear()
    }
}