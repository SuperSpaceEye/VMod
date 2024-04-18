package net.spaceeye.vmod.constraintsManaging

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.properties.ShipId

fun ServerLevel.getManagedConstraint(id: ManagedConstraintId)              = ConstraintManager.getInstance().getManagedConstraint(id)
fun ServerLevel.makeManagedConstraint(constraint: MConstraint)             = ConstraintManager.getInstance().makeConstraint(this, constraint)
fun ServerLevel.removeManagedConstraint(constraint: MConstraint)           = ConstraintManager.getInstance().removeConstraint(this, constraint.mID)
fun ServerLevel.removeManagedConstraint(constraintId: ManagedConstraintId) = ConstraintManager.getInstance().removeConstraint(this, constraintId)
fun ServerLevel.getAllManagedConstraintIdsOfShipId(shipId: ShipId)         = ConstraintManager.getInstance().getAllConstraintsIdOfId(shipId)

internal fun ServerLevel.makeManagedConstraintWithId(constraint: MConstraint, id: Int) = ConstraintManager.getInstance().makeConstraintWithId(this, constraint, id)