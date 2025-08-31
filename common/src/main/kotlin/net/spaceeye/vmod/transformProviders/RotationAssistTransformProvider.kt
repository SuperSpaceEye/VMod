package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class RotationAssistTransformProvider(
    var ship1: ClientShip,
    var ship2: ClientShip?,
    var spoint1: Vector3d,
    var spoint2: Vector3d,
    var gdir1: Vector3d,
    var gdir2: Vector3d,

    var angle: Ref<Double>,

    var doWork: () -> Boolean,
): ClientShipTransformProvider {
    constructor(placementTransform: PlacementAssistTransformProvider, angle: Ref<Double>, doWork: () -> Boolean):
            this(
                placementTransform.ship1,
                placementTransform.rresult2.ship as? ClientShip,
                placementTransform.spoint1,
                placementTransform.spoint2,
                placementTransform.gdir1,
                placementTransform.gdir2,
                angle,
                doWork
            )

    @OptIn(VsBeta::class)
    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform? {
        if (!doWork()) {return null}

        val newRot = (ship2?.renderTransform?.rotation?.get(Quaterniond()) ?: Quaterniond())
            .mul(Quaterniond(AxisAngle4d(angle.it, gdir2.toJomlVector3d())))
            .mul(getQuatFromDir(gdir2))
            .mul(getQuatFromDir(gdir1))
            .normalize()

        val transform = shipTransform.rebuild {
            this.rotation(newRot)
        }

        val position = (if (ship2 != null) posShipToWorldRender(ship2, spoint2) else spoint2) - (
            posShipToWorldRender(ship1, spoint1, transform) -
            posShipToWorldRender(ship1, Vector3d(shipTransform.positionInShip), transform)
        )

        return ShipTransformImpl.create(
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