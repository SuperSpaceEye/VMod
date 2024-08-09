package net.spaceeye.vmod.shipForceInducers

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.physgun.PlayerPhysgunState
import net.spaceeye.vmod.physgun.playerRotToQuat
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl


@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class PhysgunController: ShipForcesInducer {
    @JsonIgnore
    var sharedState: PlayerPhysgunState? = null
    override fun applyForces(physShip: PhysShip) {}

    override fun applyForcesAndLookupPhysShips(physShip: PhysShip, lookupPhysShip: (ShipId) -> PhysShip?) {
        val state = sharedState ?: return

        val lock = state.lock
        if (!lock.tryLock()) {return}

        if (state.mainShipId == -1L) {
            sharedState = null
            return lock.unlock()
        }

        physShip as PhysShipImpl
        val player = state.serverPlayer!!
        state.playerPos = Vector3d(player.eyePosition)
        state.playerDir = Vector3d(player.lookAngle).snormalize()

        val newPlayerRot = playerRotToQuat(player.xRot.toDouble(), player.yRot.toDouble())
        val deltaRot = newPlayerRot.mul(state.playerLastRot.conjugate(), Quaterniond())
        state.playerLastRot = newPlayerRot
        state.idealRotation = deltaRot.mul(state.idealRotation).normalize()

        val pConst = 160.0
        val dConst = 20.0

        val idealPos = state.playerPos + state.playerDir * state.distanceFromPlayer
        val currentPos = Vector3d(physShip.transform.shipToWorld.transformPosition(state.fromPos.x, state.fromPos.y, state.fromPos.z, org.joml.Vector3d()))

        val idealPosDiff = idealPos - currentPos
        val posDiff = idealPosDiff * pConst

        val mass = physShip.inertia.shipMass
        val force = (posDiff - (Vector3d(physShip.poseVel.vel) * dConst)) * mass
        physShip.applyInvariantForce(force.toJomlVector3d())

        val rotDiff = state.idealRotation.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond()).normalize().invert()
        val rotDiffVector = Vector3d(rotDiff.x * 2.0, rotDiff.y * 2.0, rotDiff.z * 2.0).smul(pConst)
        if (rotDiff.w > 0.0) {
            rotDiffVector.smul(-1.0)
        }
        rotDiffVector -= Vector3d(physShip.poseVel.omega).smul(dConst)
        val torque = physShip.poseVel.rot.transform(
            physShip.inertia.momentOfInertiaTensor.transform(
                physShip.poseVel.rot.transformInverse(rotDiffVector.toJomlVector3d()
                )
            )
        )

        physShip.applyInvariantTorque(torque)

        lock.unlock()
    }

    companion object {
        fun getOrCreate(ship: ServerShip) =
            ship.getAttachment<PhysgunController>()
                ?: PhysgunController().also {
                    ship.saveAttachment(it)
                }
    }
}