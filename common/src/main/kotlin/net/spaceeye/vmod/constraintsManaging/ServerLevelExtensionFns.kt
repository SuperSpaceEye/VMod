package net.spaceeye.vmod.constraintsManaging

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.properties.ShipId

fun ServerLevel.makeManagedConstraint(constraint: MConstraint, callback: ((ManagedConstraintId?) -> Unit)) = ConstraintManager.getInstance().makeConstraint(this, constraint, callback)
fun ServerLevel.getManagedConstraint(id: ManagedConstraintId)              = ConstraintManager.getInstance().getManagedConstraint(id)
fun ServerLevel.removeManagedConstraint(constraint: MConstraint)           = ConstraintManager.getInstance().removeConstraint(this, constraint.mID)
fun ServerLevel.removeManagedConstraint(constraintId: ManagedConstraintId) = ConstraintManager.getInstance().removeConstraint(this, constraintId)
fun ServerLevel.getAllManagedConstraintIdsOfShipId(shipId: ShipId)         = ConstraintManager.getInstance().getAllConstraintsIdOfId(shipId)
fun ServerLevel.getManagedConstraintIdsOfPosition(pos: BlockPos)           = ConstraintManager.getInstance().tryGetIdsOfPosition(pos)

fun ServerLevel.disableCollisionBetween(shipId1: ShipId, shipId2: ShipId, callback: (()->Unit)? = null) = ConstraintManager.getInstance().disableCollisionBetween(this, shipId1, shipId2, callback)
fun ServerLevel.enableCollisionBetween(shipId1: ShipId, shipId2: ShipId)  = ConstraintManager.getInstance().enableCollisionBetween(this, shipId1, shipId2)
fun ServerLevel.getAllDisabledCollisionsOfId(shipId: ShipId) = ConstraintManager.getInstance().getAllDisabledCollisionsOfId(shipId)

internal fun ServerLevel.makeManagedConstraintWithId(constraint: MConstraint, id: Int, callback: ((ManagedConstraintId?) -> Unit)) = ConstraintManager.getInstance().makeConstraintWithId(this, constraint, id, callback)