package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joml.Vector3d
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer

//TODO remove in the future
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class NOOP: ShipForcesInducer {
    override fun applyForces(physShip: PhysShip) {
//        physShip.applyInvariantForce(Vector3d(0.0, 1e-5, 0.0))
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<NOOP>()
                ?: NOOP().also {
                    ship.setAttachment(it)
                }
    }
}