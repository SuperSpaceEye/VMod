package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.compat.vsBackwardsCompat.mass
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import org.valkyrienskies.core.api.ships.*

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class DebugAttachment(): ShipForcesInducer {
    override fun applyForces(physShip: PhysShip) {
        val forceDiff = (Vector3d(0, 10, 0) - VS_DEFAULT_GRAVITY) * physShip.mass
        if (forceDiff.sqrDist() < Float.MIN_VALUE) return

        physShip.applyInvariantForce(forceDiff.toJomlVector3d())
    }

    companion object {
        val VS_DEFAULT_GRAVITY = Vector3d(0, -10, 0)

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<DebugAttachment>()
                ?: DebugAttachment().also {
                    ship.setAttachment(it)
                }
    }
}