package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.posShipToWorldRender
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class RotationAssistTransformProvider(
    var ship: ClientShip,
    var spoint1: Vector3d,
    var rpoint2: Vector3d,
    var rotation1: Quaterniond,
    var rotation2: Quaterniond,
    var dir: Vector3d,

    var angle: Ref<Double>
): ClientShipTransformProvider {
    constructor(placementTransform: PlacementAssistTransformProvider, angle: Ref<Double>):
            this(
                placementTransform.ship1,
                placementTransform.spoint1,
                placementTransform.rpoint2,
                placementTransform.rotation1,
                placementTransform.rotation2,
                placementTransform.dir2,
                angle
            )

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        shipTransform as ShipTransformImpl

        val angle = Quaterniond(AxisAngle4d(angle.it, dir.toJomlVector3d()))

        val rotation1 = Quaterniond(rotation1)
        val rotation2 = Quaterniond(rotation2)

        val newRot = Quaterniond()
            .mul(angle)
            .mul(rotation2)
            .mul(rotation1)
            .normalize()

        val transform = shipTransform.copy(shipToWorldRotation = newRot)

        val position = rpoint2 - (
            posShipToWorldRender(ship, spoint1, transform) -
            posShipToWorldRender(ship, Vector3d(shipTransform.positionInShip), transform)
        )

        return ShipTransformImpl(
            position.toJomlVector3d(),
            shipTransform.positionInShip,
            newRot,
            shipTransform.shipToWorldScaling
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