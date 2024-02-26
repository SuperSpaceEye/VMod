package net.spaceeye.vsource.constraintsSaving

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsSaving.types.MConstraint
import net.spaceeye.vsource.utils.MPair
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint

internal class LoadingGroup(
    val level: ServerLevel,
    val constraintsToLoad: MutableList<MConstraint>,
    val neededShipIds: MutableSet<ShipId>,
    val shipIsStaticStatus: MutableMap<ShipId, Boolean>
) {
    private val shipRefs: MutableMap<ShipId, ServerShip> = mutableMapOf()

    fun setLoadedId(ship: ServerShip) {
        if (neededShipIds.isEmpty()) {return}
        if (!neededShipIds.remove(ship.id)) { return }

        shipRefs.computeIfAbsent(ship.id) {ship}
        shipIsStaticStatus.computeIfAbsent(ship.id) {ship.isStatic}
        ship.isStatic = true // so that ships don't drift while ships are being loaded

        if (neededShipIds.isEmpty()) {
            applyConstraints()

            constraintsToLoad.clear()
            shipRefs.clear()
        }
    }

    private fun applyConstraints() {
        for (constraint in constraintsToLoad) {
            level.makeManagedConstraintWithId(constraint, constraint.mID.id)
        }
        for ((k, ship) in shipRefs) {
            ship.isStatic = shipIsStaticStatus[ship.id] ?: continue
            shipIsStaticStatus.remove(ship.id)
        }
    }
}