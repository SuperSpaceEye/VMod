package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.physgun.PlayerPhysgunState
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.rotateVecByQuat
import org.joml.Quaterniond
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipId


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

        val pConst = state.pConst
        val dConst = state.dConst
        val iConst = state.iConst

        val lock = state.lock
        if (!lock.tryLock()) {return}

        if (state.mainShipId == -1L) {
            sharedState = null
            return lock.unlock()
        }

        val shipsToInfluence = mutableListOf<PhysShip>()
        state.caughtShipIds.forEach { shipsToInfluence.add(lookupPhysShip(it) ?: return@forEach) }

        val idealPos = state.playerPos + state.playerDir * state.distanceFromPlayer
        val currentPos = Vector3d(physShip.transform.shipToWorld.transformPosition(state.fromPos.x, state.fromPos.y, state.fromPos.z, org.joml.Vector3d()))

        val idealPosDiff = idealPos - currentPos
        val posDiff = idealPosDiff * pConst


        val mass = (physShip).mass
        val force = (posDiff - (Vector3d(physShip.velocity) * dConst)) * mass
        physShip.applyInvariantForce(force.toJomlVector3d())

        val rotDiff = state.idealRotation.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond()).normalize().invert()
        val rotDiffVector = Vector3d(rotDiff.x * 2.0, rotDiff.y * 2.0, rotDiff.z * 2.0).smul(pConst)
        if (rotDiff.w > 0.0) {
            rotDiffVector.smul(-1.0)
        }
        rotDiffVector -= Vector3d(physShip.omega).smul(dConst)

        val torque = physShip.transform.shipToWorldRotation.transform(
                physShip.momentOfInertia.transform(
                    physShip.transform.shipToWorldRotation.transformInverse(rotDiffVector.toJomlVector3d())
                )
            )

        physShip.applyInvariantTorque(torque)

        shipsToInfluence
            .forEach {
                val dir = ((idealPos - Vector3d(it.transform.positionInWorld))).toJomlVector3d()
                val rotatedDir = rotateVecByQuat(dir, rotDiff)
                val diff = Vector3d(rotatedDir.sub(dir)) * iConst

                val mass = it.mass
                val force = (diff + posDiff - (Vector3d(it.velocity) * dConst)) * mass
                it.applyInvariantForce(force.toJomlVector3d())
            }

        lock.unlock()
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<PhysgunController>()
                ?: PhysgunController().also {
                    ship.setAttachment(it)
                }
    }
}