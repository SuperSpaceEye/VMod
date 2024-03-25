package net.spaceeye.vmod.transformProviders

import net.minecraft.client.Minecraft
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.utils.*
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.getShipObjectManagingPos

class RotationAssistTransformProvider(
    var ship1: ClientShip,
    var ship2: ClientShip?,
    var spoint1: Vector3d,
    var spoint2: Vector3d,
    var gdir1: Vector3d,
    var gdir2: Vector3d,

    var angle: Ref<Double>
): ClientShipTransformProvider {
    constructor(placementTransform: PlacementAssistTransformProvider, angle: Ref<Double>):
            this(
                placementTransform.ship1,
                Minecraft.getInstance().level.getShipObjectManagingPos(placementTransform.rresult2.blockPosition),
                placementTransform.spoint1,
                placementTransform.spoint2,
                placementTransform.gdir1,
                placementTransform.gdir2,
                angle
            )

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform? {
        //TODO think of a better way
        if (!ToolgunItem.playerIsUsingToolgun()) {return null}
        shipTransform as ShipTransformImpl

        val dir2 = if (ship2 != null) { transformDirectionShipToWorldRender(ship2!!, gdir2) } else {gdir2}

        val angle = Quaterniond(AxisAngle4d(angle.it, dir2.toJomlVector3d()))

        val newRot = Quaterniond()
            .mul(angle)
            .mul(getQuatFromDir(dir2))
            .mul(getQuatFromDir(gdir1))
            .normalize()

        val transform = shipTransform.copy(shipToWorldRotation = newRot)

        val position = (if (ship2 != null) posShipToWorldRender(ship2, spoint2) else spoint2) - (
            posShipToWorldRender(ship1, spoint1, transform) -
            posShipToWorldRender(ship1, Vector3d(shipTransform.positionInShip), transform)
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