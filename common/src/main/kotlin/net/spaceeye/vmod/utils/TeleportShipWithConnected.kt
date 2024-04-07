package net.spaceeye.vmod.utils

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.constraintsManaging.getManagedConstraint
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.shipObjectWorld

//TODO scaling is a bit fucky
fun teleportShipWithConnected(
    level: ServerLevel,
    mainShip: ServerShip,
    pos: Vector3d,
    rotation: Quaterniondc,
    scale: Double? = null,

    maintainRelativeScale: Boolean = false
) {
    val scale = scale ?: Vector3d(mainShip.transform.shipToWorldScaling).avg()

    val traversed = VSConstraintsKeeper.traverseGetConnectedShips(mainShip.id)
    val transform = (mainShip.transform as ShipTransformImpl).copy(pos.toJomlVector3d(), shipToWorldRotation = rotation, shipToWorldScaling = org.joml.Vector3d(scale, scale, scale))

    traversed.traversedShipIds.forEach {
        if (it == mainShip.id) { return@forEach }

        val otherShip = level.shipObjectWorld.loadedShips.getById(it) ?: return@forEach
        val shipPosInMainShipyard = posWorldToShip(mainShip, Vector3d(otherShip.transform.positionInWorld))

        val newPos = posShipToWorld(null, shipPosInMainShipyard, transform)
        val diff = rotation.mul(mainShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())

        val newRotation = diff.mul(otherShip.transform.shipToWorldRotation)

        val ratio = Vector3d(otherShip.transform.shipToWorldScaling).avg() / Vector3d(mainShip.transform.shipToWorldScaling).avg()

//        val newScale = if (maintainRelativeScale) {scale * ratio} else {scale}
        val newScale = scale

        level.shipObjectWorld.teleportShip(
            otherShip, ShipTeleportDataImpl(
                newPos.toJomlVector3d(), newRotation, org.joml.Vector3d(), newScale = newScale
            )
        )
    }

    level.shipObjectWorld.teleportShip(
        mainShip, ShipTeleportDataImpl(
            pos.toJomlVector3d(), rotation, org.joml.Vector3d(), newScale = scale
        )
    )

    traversed.traversedMConstraintIds.forEach { level.getManagedConstraint(ManagedConstraintId(it))?.onScale(level, scale) }
}