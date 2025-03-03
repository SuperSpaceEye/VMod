package net.spaceeye.vmod.utils.vs

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.getVEntity
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld

fun getMinScale(level: ServerLevel, traversedData: TraversedData): Double {
    return traversedData.traversedShipIds.mapNotNull {
        val ship = level.shipObjectWorld.allShips.getById(it) ?: return@mapNotNull null
        Vector3d(ship.transform.shipToWorldScaling).avg()
    }.min()
}

@OptIn(VsBeta::class)
fun teleportShipWithConnected(
    level: ServerLevel,
    mainShip: ServerShip,
    pos: Vector3d,
    rotation: Quaterniondc,
    scale: Double? = null,
    dimensionId: DimensionId? = null
) {
    val traversed = traverseGetConnectedShips(mainShip.id)

    val minScale = getMinScale(level, traversed)
    val scale = scale ?: minScale
    val scaleBy = if (minScale != scale) { scale / minScale } else { 1.0 }

    val transform = mainShip.transform.rebuild {
        this.position(pos.toJomlVector3d())
        this.rotation(Quaterniond(rotation))
        this.scaling (org.joml.Vector3d(1.0, 1.0, 1.0).mul(Vector3d(mainShip.transform.shipToWorldScaling).avg() * scaleBy))
    }

    traversed.traversedShipIds.forEach {
        if (it == mainShip.id) { return@forEach }

        val otherShip = level.shipObjectWorld.loadedShips.getById(it) ?: return@forEach
        val shipPosInMainShipyard = posWorldToShip(mainShip, Vector3d(otherShip.transform.positionInWorld))

        val newPos = posShipToWorld(null, shipPosInMainShipyard, transform)
        val diff = rotation.mul(mainShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())

        val newRotation = diff.mul(otherShip.transform.shipToWorldRotation)

        val newScale = Vector3d(otherShip.transform.shipToWorldScaling).avg() * scaleBy

        level.shipObjectWorld.teleportShip(
            otherShip, ShipTeleportDataImpl(
                newPos.toJomlVector3d(), newRotation, otherShip.velocity, otherShip.omega, dimensionId ?: otherShip.chunkClaimDimension, newScale
            )
        )
    }

    level.shipObjectWorld.teleportShip(
        mainShip, ShipTeleportDataImpl(
            pos.toJomlVector3d(), rotation, mainShip.velocity, mainShip.omega, dimensionId ?: mainShip.chunkClaimDimension, Vector3d(mainShip.transform.shipToWorldScaling).avg() * scaleBy
        )
    )

    if (scaleBy != 1.0) { traversed.traversedVEntityIds.forEach { level.getVEntity(it)?.onScaleBy(level, scaleBy, Vector3d(mainShip.transform.positionInWorld)) } }
}