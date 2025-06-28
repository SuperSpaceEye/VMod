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
    var useDimensionGravity = true

    @JsonIgnore
    var dimensionGravity = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
    @JsonIgnore
    var gravityVector = dimensionGravity

    private var gravityVectorForSaving: JVector3d
        get() = gravityVector.toJomlVector3d()
        set(value) { gravityVector = Vector3d(value) }

    override fun applyForces(physShip: PhysShip) {
        val gravityVector = if (useDimensionGravity) dimensionGravity else gravityVector
        val forceDiff = (gravityVector - VS_DEFAULT_GRAVITY) * physShip.mass
        if (forceDiff.sqrDist() < Float.MIN_VALUE) return

        physShip.applyInvariantForce(forceDiff.toJomlVector3d())
    }

    fun reset() {
        gravityVector = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
        useDimensionGravity = true
    }

    @JsonIgnore
    fun effectiveGravity() = if (useDimensionGravity) dimensionGravity else gravityVector

    companion object {
        val VS_DEFAULT_GRAVITY = Vector3d(0, -10, 0)

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<GravityController>()
                ?: GravityController(ship.chunkClaimDimension).also {
                    ship.saveAttachment(it)
                }
    }
}