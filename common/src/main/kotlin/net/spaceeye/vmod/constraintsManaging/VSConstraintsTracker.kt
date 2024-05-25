package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.mixin.ShipObjectWorldAccessor
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

object VSConstraintsTracker: ServerClosable() {
    val idToShips = mutableMapOf<Int, Pair<ShipId, ShipId>>()
    val shipToIds = mutableMapOf<ShipId, MutableList<Int>>()

    fun addNewConstraint(constraint: VSConstraint, constraintId: VSConstraintId) {
        shipToIds.getOrPut(constraint.shipId0) { mutableListOf() }.add(constraintId)
        shipToIds.getOrPut(constraint.shipId1) { mutableListOf() }.add(constraintId)

        idToShips[constraintId] = Pair(constraint.shipId0, constraint.shipId1)
    }

    fun removeConstraint(constraint: VSConstraint, constraintId: ConstraintId) {
        val one = shipToIds[constraint.shipId0]
        if (one != null) {one.remove(constraintId); if (one.isEmpty()) {shipToIds.remove(constraint.shipId0)}}

        val two = shipToIds[constraint.shipId1]
        if (two != null) {two.remove(constraintId); if (two.isEmpty()) {shipToIds.remove(constraint.shipId1)}}

        idToShips.remove(constraintId)
    }

    fun updateConstraint(constraint: VSConstraint, constraintId: ConstraintId) {
        val shipIds = idToShips[constraintId] ?: run { ELOG("THIS SHOULD NEVER HAPPEN. IN VSConstraintsKeeper.updateConstraint idToShip with ${constraintId} IS null.") ; null } ?: return
        if (shipIds.first == constraint.shipId0 && shipIds.second == constraint.shipId1) { return }

        removeConstraint(constraint, constraintId)
        addNewConstraint(constraint, constraintId)
    }

    fun getVSConstraints(ids: List<VSConstraintId>): List<Pair<VSConstraintId, VSConstraint?>> {
        val acc = ServerLevelHolder.server!!.shipObjectWorld as ShipObjectWorldAccessor
        return ids.map { Pair(it, acc.constraintsAcc[it]) }
    }

    fun getIdsOfShip(shipId: ShipId): List<Int> {
        return shipToIds.getOrDefault(shipId, listOf())
    }

    override fun close() {
        shipToIds.clear()
        idToShips.clear()
    }
}