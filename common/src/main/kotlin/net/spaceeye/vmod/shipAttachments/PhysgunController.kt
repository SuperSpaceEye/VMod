package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.physgun.PlayerPhysgunState
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.rotateVecByQuat
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.PhysLevel


@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class PhysgunController: ShipPhysicsListener {
    @JsonIgnore
    var sharedState: PlayerPhysgunState? = null

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
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
        state.caughtShipIds.forEach { shipsToInfluence.add(physLevel.getShipById(it) ?: return@forEach) }
//        shipsToInfluence.add(physShip)

        val idealPos = state.playerPos + state.playerDir * state.distanceFromPlayer
        val currentPos = Vector3d(physShip.transform.shipToWorld.transformPosition(state.fromPos.x, state.fromPos.y, state.fromPos.z, org.joml.Vector3d()))

        val idealPosDiff = idealPos - currentPos
        val posDiff = idealPosDiff * pConst

//            val totalMass = shipsToInfluence.map { it.mass }.reduce { acc, d -> acc+d }
//            val weightedCenter = shipsToInfluence
//                .map { (Vector3d(it.transform.positionInModel) - getCenterPos(it.transform.positionInModel)) * (it.mass / totalMass)}
//                .reduce {acc, add -> acc + add}

//            val posDiffs = shipsToInfluence.map {
//                val idealPosDiff = idealPos - currentPos
//                idealPosDiff * pConst
//            }

//            shipsToInfluence//.zip(posDiffs)
//                .forEach {// (
//                               it
//                            //       , posDiff)
//                    ->
//
//                val force = (posDiff - (Vector3d(it.velocity) * dConst)) * it.mass
//                it.applyInvariantForce(force.toJomlVector3d())
//            }
//        }
//
//        run {
//            val totalMass = shipsToInfluence.map { it.mass }.reduce { acc, d -> acc+d }
//            val weightedCenter = shipsToInfluence
//                .map { posShipToWorld(it, Vector3d(it.transform.positionInModel)) * (it.mass / totalMass) }
//                .reduce {acc, add -> acc + add}
//
//            val weightCenters = shipsToInfluence.map { posWorldToShip(it, weightedCenter) }
//            val idkDirections = weightCenters.zip(shipsToInfluence).map {(wcenter, ship) ->
//                (Vector3d(ship.transform.positionInModel) - wcenter).normalize()
//            }
//
//            val forcePositions = idkDirections.zip(shipsToInfluence).map { (dir, ship) -> Vector3d(ship.transform.positionInModel) + dir }
//
//            val targetPositions = forcePositions.zip(shipsToInfluence).map { (pos, ship) ->
//                val rotDif = state.idealRotation
//                .mul(
//                    physShip.transform.shipToWorldRotation
//                        .invert(Quaterniond()), Quaterniond())
//                .normalize()
////                .invert()
//
//                posWorldToShip(ship,
//                    Vector3d(rotDif
//                        .transform(
//                            (posShipToWorld(ship, pos) - weightedCenter).toJomlVector3d()
//                        )
//                    ) + weightedCenter
//                )
//            }
//
//            val perpDirections = idkDirections.zip(forcePositions.zip(targetPositions)).map {
//                (dir, positions) ->
//                val (current, target) = positions
//                val t = (target - current).dot(dir) / dir.dot(dir)
//                val closestPointOnLine = current + dir * t
//                (target - closestPointOnLine).normalize()
//            }
//
//            val diffs = targetPositions.zip(forcePositions).map { (a, b) -> println("${a - b}"); (a - b).dist() * pConst }
//
//            shipsToInfluence.zip(diffs.zip(forcePositions.zip(perpDirections))).forEach {
//                (ship, temp) ->
//                val (force, temp2) = temp
//                val (pos, dir) = temp2
//
//                if (!dir.x.isFinite() || !dir.y.isFinite() || !dir.z.isFinite()) {return@forEach println("1FUCK")}
//                if (!pos.x.isFinite() || !pos.y.isFinite() || !pos.z.isFinite()) {return@forEach println("2FUCK")}
////                ship.applyRotDependentForceToPos((dir * force * ship.mass).toJomlVector3d(), pos.toJomlVector3d())
//
//                println("(%.2f %.2f %.2f) %.2f".format(dir.x, dir.y, dir.z, force))
//            }
//
////            println(targetPositions.zip(forcePositions).map { (a, b) -> a - b })
//        }
//
//        return lock.unlock()
//
//        run {
////            val rotDif = state.idealRotation
////                .mul(
////                    physShip.transform.shipToWorldRotation
////                        .invert(Quaterniond()), Quaterniond())
////                .normalize()
////                .invert()
//            val rotDif = state.idealRotation.normalize(Quaterniond()).invert()
//            // pConst is
//            val rotDifVector = org.joml.Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0).mul(pConst)
//            if (rotDif.w() < 0) {
//                rotDifVector.mul(-1.0)
//            }
//            rotDifVector.mul(-1.0)
//
//            // Integrate
//            rotDifVector.sub(physShip.angularVelocity.mul(dConst, org.joml.Vector3d()))
//
//            val torque = physShip.transform.shipToWorldRotation.transform(
//                physShip.momentOfInertia.transform(
//                    physShip.transform.shipToWorldRotation.transformInverse(
//                        rotDifVector,
//                        org.joml.Vector3d()
//                    )
//                )
//            )
//            physShip.applyInvariantTorque(torque)
//        }
//
//        return lock.unlock()

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
            ship.getAttachment(PhysgunController::class.java)
                ?: PhysgunController().also {
                    ship.setAttachment(it)
                }
    }
}