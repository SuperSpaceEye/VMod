package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.posShipToWorldRender
import net.spaceeye.vmod.utils.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class CenteredAroundPlacementAssistTransformProvider(
    var mainTransformProvider: PlacementAssistTransformProvider,
    var ship: ClientShip
): ClientShipTransformProvider {
    val shipPosInMainShipShipyard: Vector3d

    init {
        val mainShip = mainTransformProvider.ship1

        // transform position of other ship to shipyard, so that when using posShipToWorld with modified transform
        // the coordinate the other ship should be would be calculated
        shipPosInMainShipShipyard = posWorldToShip(mainShip, Vector3d(ship.transform.positionInWorld))
        mainTransformProvider.ignoreShipIds.add(ship.id)
    }


    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        shipTransform as ShipTransformImpl
        val mainShip = mainTransformProvider.ship1

        // render transform of main ship should already be properly rotated and moved
        val newPos = posShipToWorldRender(mainShip, shipPosInMainShipShipyard)
        // so what is the idea? to properly rotate the other ship, get the difference between original rotation and
        // new rotation, and add it to other ship.
        val diff = mainShip.renderTransform.shipToWorldRotation.mul(mainShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
        val rotation = diff.mul(ship.transform.shipToWorldRotation)

        return shipTransform.copy(
            newPos.toJomlVector3d(),
            shipToWorldRotation = rotation
        )
    }

    override fun provideNextTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        latestNetworkTransform: ShipTransform
    ): ShipTransform? {
        return null
    }
}