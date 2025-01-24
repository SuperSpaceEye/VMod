package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.rebuild
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform

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


    @OptIn(VsBeta::class)
    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        val mainShip = mainTransformProvider.ship1

        // render transform of main ship should already be properly rotated and moved
        val newPos = posShipToWorldRender(mainShip, shipPosInMainShipShipyard)
        // so what is the idea? to properly rotate the other ship, get the difference between original rotation and
        // new rotation, and add it to other ship.
        val diff = mainShip.renderTransform.shipToWorldRotation.mul(mainShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
        val rotation = diff.mul(ship.transform.shipToWorldRotation)

        return shipTransform.rebuild {
            this.position(newPos.toJomlVector3d())
            this.rotation(rotation)
        } as ShipTransform
    }

    override fun provideNextTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        latestNetworkTransform: ShipTransform
    ): ShipTransform? {
        return null
    }
}