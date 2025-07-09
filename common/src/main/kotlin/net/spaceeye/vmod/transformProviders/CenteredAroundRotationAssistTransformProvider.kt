package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.joml.Quaterniond
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.rebuild
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform

class CenteredAroundRotationAssistTransformProvider(
    val shipPosInMainShipyard: Vector3d,
    val mainShip: ClientShip,
    val ship: ClientShip
): ClientShipTransformProvider {
    constructor(tp: CenteredAroundPlacementAssistTransformProvider):
            this(tp.shipPosInMainShipShipyard,
                 tp.mainTransformProvider.ship1,
                 tp.ship)

    @OptIn(VsBeta::class)
    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        val newPos = posShipToWorldRender(mainShip, shipPosInMainShipyard)
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