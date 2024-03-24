package net.spaceeye.vmod.transformProviders

import net.spaceeye.vmod.WLOG
import org.joml.AxisAngle4d
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform

class PeekerTransformProvider(val name: String = ""): ServerShipTransformProvider {
    override fun provideNextTransformAndVelocity(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform
    ): ServerShipTransformProvider.NextTransformAndVelocityData? {
        val axis = AxisAngle4d(shipTransform.shipToWorldRotation)
        WLOG("$name $axis")

        return null
    }
}