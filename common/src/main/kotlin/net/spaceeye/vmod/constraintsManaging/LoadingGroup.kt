package net.spaceeye.vmod.constraintsManaging

import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld

internal data class ShipData(var isStatic: Boolean, var velocity: Vector3d, var omega: Vector3d) {
    companion object {
        @JvmStatic
        fun fromShip(ship: ServerShip): ShipData {
            return ShipData(ship.isStatic, Vector3d(ship.velocity), Vector3d(ship.omega))
        }
    }
}

internal class LoadingGroup(
    val level: ServerLevel,
    val constraintsToLoad: MutableList<MConstraint>,
    val neededShipIds: MutableSet<ShipId>,
    val shipDataStatus: MutableMap<ShipId, ShipData>
) {
    private val shipRefs: MutableMap<ShipId, ServerShip> = mutableMapOf()

    // flag used in ConstraintManager in saveNotLoadedConstraints and nowhere else
    var wasSaved = false

    fun setLoadedId(ship: ServerShip) {
        if (neededShipIds.isEmpty()) {return}
        if (!neededShipIds.remove(ship.id)) { return }

        shipRefs.computeIfAbsent(ship.id) {ship}
        shipDataStatus.computeIfAbsent(ship.id) {ShipData.fromShip(ship)}
        ship.isStatic = true // so that ships don't drift while ships are being loaded

        if (neededShipIds.isEmpty()) {
            applyConstraints()

            constraintsToLoad.clear()
            shipRefs.clear()
        }
    }

    private fun applyConstraints() {
        val finishFn = {
            for ((_, ship) in shipRefs) {
                val data = shipDataStatus[ship.id] ?: continue

                ship.isStatic = data.isStatic
                level.shipObjectWorld.teleportShip(ship, ShipTeleportDataImpl(
                    ship.transform.positionInWorld, ship.transform.shipToWorldRotation,
                    data.velocity, data.omega, ship.chunkClaimDimension, ship.transform.shipToWorldScaling.x()
                ))

                shipDataStatus.remove(ship.id)
            }
        }

        var numToLoad = constraintsToLoad.size
        for (constraint in constraintsToLoad) {
            level.makeManagedConstraintWithId(constraint, constraint.mID) {
                numToLoad--
                if (numToLoad > 0) return@makeManagedConstraintWithId
                finishFn()
            }
        }
    }
}