package net.spaceeye.vsource.utils.constraintsSaving

import net.minecraft.server.level.ServerLevel
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint

fun ServerLevel.makeManagedConstraint(constraint: VSConstraint)                                      = ConstraintManager.getInstance(this).makeConstraint(this, constraint)
fun ServerLevel.removeManagedConstraint(constraintId: ManagedConstraintId)                           = ConstraintManager.getInstance(this).removeConstraint(this, constraintId)
fun ServerLevel.updateManagedConstraint(constraintId: ManagedConstraintId, constraint: VSConstraint) = ConstraintManager.getInstance(this).updateConstraint(this, constraintId, constraint)
fun ServerLevel.getAllManagedConstraintsOfId(shipId: ShipId)                                         = ConstraintManager.getInstance(this).getAllConstraintsIdOfId(this, shipId)

@ApiStatus.Internal
fun ServerLevel.makeManagedConstraintWithId(constraint: VSConstraint, id: Int) = ConstraintManager.getInstance(this).makeConstraintWithId(this, constraint, id)