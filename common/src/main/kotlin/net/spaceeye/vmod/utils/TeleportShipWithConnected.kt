package net.spaceeye.vmod.utils

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.shipObjectWorld

fun teleportShipWithConnected(
    level: ServerLevel,
    mainShip: ServerShip,
    pos: Vector3d,
    rotation: Quaterniond
) {
    VSConstraintsKeeper.traverseGetConnectedShips(mainShip.id).forEach {
        if (it == mainShip.id) {
            return@forEach
        }

        val otherShip = level.shipObjectWorld.loadedShips.getById(it) ?: return@forEach
        val shipPosInMainShipyard = posWorldToShip(mainShip, Vector3d(otherShip.transform.positionInWorld))

        val newPos = posShipToWorld(null, shipPosInMainShipyard, (mainShip.transform as ShipTransformImpl).copy(pos.toJomlVector3d(), shipToWorldRotation = rotation))
        val diff = rotation.mul(mainShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())

        val newRotation = diff.mul(otherShip.transform.shipToWorldRotation)

        level.shipObjectWorld.teleportShip(
            otherShip, ShipTeleportDataImpl(
                newPos.toJomlVector3d(), newRotation, org.joml.Vector3d()
            )
        )
    }


    level.shipObjectWorld.teleportShip(
        mainShip, ShipTeleportDataImpl(
            pos.toJomlVector3d(), rotation, org.joml.Vector3d()
        )
    )
}