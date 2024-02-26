package net.spaceeye.vsource.constraintsSaving

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsSaving.types.MConstraint
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint

fun ServerLevel.makeManagedConstraint(constraint: MConstraint)             = ConstraintManager.getInstance().makeConstraint(this, constraint)
fun ServerLevel.removeManagedConstraint(constraint: MConstraint)           = ConstraintManager.getInstance().removeConstraint(this, constraint.mID)
fun ServerLevel.removeManagedConstraint(constraintId: ManagedConstraintId) = ConstraintManager.getInstance().removeConstraint(this, constraintId)
fun ServerLevel.getAllManagedConstraintIdsOfShipId(shipId: ShipId)         = ConstraintManager.getInstance().getAllConstraintsIdOfId(this, shipId)

internal fun ServerLevel.makeManagedConstraintWithId(constraint: MConstraint, id: Int) = ConstraintManager.getInstance().makeConstraintWithId(this, constraint, id)