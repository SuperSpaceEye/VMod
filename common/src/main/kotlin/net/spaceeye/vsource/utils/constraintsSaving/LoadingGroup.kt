package net.spaceeye.vsource.utils.constraintsSaving

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.utils.MPair
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint

internal class LoadingGroup(
    val level: ServerLevel,
    val constraintsToLoad: MutableList<MPair<VSConstraint, Int>>,
    val neededShipIds: MutableSet<ShipId>,
    val shipIsStaticStatus: MutableMap<ShipId, Boolean>
) {
    //boolean is for isStatic status before loading
    private val shipRefs: MutableMap<ShipId, ServerShip> = mutableMapOf()
    var isLoaded = false

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
            isLoaded = true
        }
    }

    private fun applyConstraints() {
        for (constraint in constraintsToLoad) {
            level.makeManagedConstraintWithId(constraint.first, constraint.second)
        }
        for ((k, ship) in shipRefs) {
            ship.isStatic = shipIsStaticStatus[ship.id] ?: continue
            shipIsStaticStatus.remove(ship.id)
        }
    }
}