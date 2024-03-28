package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.posShipToWorldRender
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class CenteredAroundRotationAssistTransformProvider(
    val shipPosInMainShipyard: Vector3d,
    val mainShip: ClientShip,
    val ship: ClientShip
): ClientShipTransformProvider {
    constructor(tp: CenteredAroundPlacementAssistTransformProvider):
            this(tp.shipPosInMainShipShipyard,
                 tp.mainTransformProvider.ship1,
                 tp.ship)

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        shipTransform as ShipTransformImpl

        val newPos = posShipToWorldRender(mainShip, shipPosInMainShipyard)
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