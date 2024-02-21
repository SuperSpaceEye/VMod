package net.spaceeye.vsource.constraintsSaving

import net.minecraft.server.level.ServerLevel
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint

fun ServerLevel.makeManagedConstraint(constraint: VSConstraint)                                      = ConstraintManager.getInstance().makeConstraint(this, constraint)
fun ServerLevel.removeManagedConstraint(constraintId: ManagedConstraintId)                           = ConstraintManager.getInstance().removeConstraint(this, constraintId)
fun ServerLevel.updateManagedConstraint(constraintId: ManagedConstraintId, constraint: VSConstraint) = ConstraintManager.getInstance().updateConstraint(this, constraintId, constraint)
fun ServerLevel.getAllManagedConstraintIdsOfShipId(shipId: ShipId)                                   = ConstraintManager.getInstance().getAllConstraintsIdOfId(this, shipId)

@ApiStatus.Internal
fun ServerLevel.makeManagedConstraintWithId(constraint: VSConstraint, id: Int) = ConstraintManager.getInstance().makeConstraintWithId(this, constraint, id)