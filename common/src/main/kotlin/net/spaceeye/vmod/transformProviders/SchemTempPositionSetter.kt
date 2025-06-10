package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.compat.vsBackwardsCompat.scaling
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class SchemTempPositionSetter(
    val ship: ServerShip,
    val toPos: Vector3d,
    val relPos: Vector3d,
): ServerShipTransformProvider {
    override fun provideNextTransformAndVelocity(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform
    ): ServerShipTransformProvider.NextTransformAndVelocityData? {
        return ServerShipTransformProvider.NextTransformAndVelocityData(
            ShipTransformImpl(
                (relPos + toPos + Vector3d(shipTransform.positionInShip) - (ship.shipAABB?.let {b ->Vector3d(
                    (b.maxX() - b.minX()) / 2.0 + b.minX(),
                    (b.maxY() - b.minY()) / 2.0 + b.minY(),
                    (b.maxZ() - b.minZ()) / 2.0 + b.minZ(),
                )} ?: return null)).toJomlVector3d(),
                shipTransform.positionInShip,
                shipTransform.shipToWorldRotation,
                shipTransform.scaling
            ), JVector3d(0.0, 0.0, 0.0), JVector3d(0.0, 0.0, 0.0)
        )
    }
}