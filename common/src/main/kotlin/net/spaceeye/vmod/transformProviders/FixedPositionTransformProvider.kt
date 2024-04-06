package net.spaceeye.vmod.transformProviders

import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class FixedPositionTransformProvider(
        var positionInWorld: org.joml.Vector3dc,
        var positionInShip: org.joml.Vector3dc
): ServerShipTransformProvider {
    override fun provideNextTransformAndVelocity(prevShipTransform: ShipTransform, shipTransform: ShipTransform): ServerShipTransformProvider.NextTransformAndVelocityData? {
        shipTransform as ShipTransformImpl
        return ServerShipTransformProvider.NextTransformAndVelocityData(
                shipTransform.copy(
                        positionInWorld,
                        positionInShip,
                        shipTransform.shipToWorldRotation,
                        shipTransform.shipToWorldScaling
                ),
                Vector3d(),
                Vector3d()
        )
    }
}