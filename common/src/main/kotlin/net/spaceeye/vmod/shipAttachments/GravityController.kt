package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.compat.vsBackwardsCompat.mass
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vsStuff.VSGravityManager
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import org.valkyrienskies.core.api.ships.*

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GravityController(
    var dimensionId: String,
): ShipForcesInducer {
    @JsonIgnore
    var gravityVector = VSGravityManager.getDimensionGravityMutableReference(dimensionId)

    private var gravityVectorForSaving: JVector3d
        get() = gravityVector.toJomlVector3d()
        set(value) {
            gravityVector = Vector3d(value)
            // a bit dumb but should work
            val dimensionGravity = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
            if (gravityVector == dimensionGravity) { gravityVector = dimensionGravity }
        }

    override fun applyForces(physShip: PhysShip) {
        val forceDiff = (gravityVector - VS_DEFAULT_GRAVITY) * physShip.mass
        if (forceDiff.sqrDist() < Float.MIN_VALUE) return

        physShip.applyInvariantForce(forceDiff.toJomlVector3d())
    }

    fun reset() {
        gravityVector = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
    }

    companion object {
        val VS_DEFAULT_GRAVITY = Vector3d(0, -10, 0)

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<GravityController>()
                ?: GravityController(ship.chunkClaimDimension).also {
                    ship.setAttachment(it)
                }
    }
}