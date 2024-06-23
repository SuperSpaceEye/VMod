package net.spaceeye.vmod.shipForceInducers

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

//TODO make changeable default dimension gravity
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GravityController(
    var dimensionId: String,
    gravityVector: JVector3d = org.joml.Vector3d(0.0, -10.0, 0.0)
): ShipForcesInducer {
    @JsonIgnore
    var gravityVector = Vector3d(gravityVector)

    private var gravityVectorForSaving: JVector3d
        get() = gravityVector.toJomlVector3d()
        set(value) { gravityVector = Vector3d(value)}

    override fun applyForces(physShip: PhysShip) {
        physShip as PhysShipImpl

        val forceDiff = (gravityVector - VS_DEFAULT_GRAVITY) * physShip.inertia.shipMass

        physShip.applyInvariantForce(forceDiff.toJomlVector3d())
    }

    fun reset() {
        gravityVector = Vector3d(0, -10, 0)
    }

    companion object {
        val VS_DEFAULT_GRAVITY = Vector3d(0, -10, 0)

        fun getOrCreate(ship: ServerShip) =
            ship.getAttachment<GravityController>()
                ?: GravityController(ship.chunkClaimDimension).also {
                    ship.saveAttachment(it)
                }
    }
}