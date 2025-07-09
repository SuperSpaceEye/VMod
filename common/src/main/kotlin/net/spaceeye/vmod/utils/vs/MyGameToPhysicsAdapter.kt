package net.spaceeye.vmod.utils.vs

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple3
import net.spaceeye.vmod.utils.Tuple4
import org.joml.Vector3dc
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.core.apigame.world.PhysLevelCore
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.mod.common.dimensionId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.also

val ServerLevel.gtpa get(): MyGameToPhysicsAdapter = VM.dimToGTPA.getOrPut(this.dimensionId) { MyGameToPhysicsAdapter() }

@OptIn(VsBeta::class)
class MyGameToPhysicsAdapter {
    private val invForces    = ConcurrentLinkedQueue<Pair<ShipId, Vector3dc>>()
    private val invTorques   = ConcurrentLinkedQueue<Pair<ShipId, Vector3dc>>()
    private val rotForces    = ConcurrentLinkedQueue<Pair<ShipId, Vector3dc>>()
    private val rotTorques   = ConcurrentLinkedQueue<Pair<ShipId, Vector3dc>>()
    private val invPosForces = ConcurrentLinkedQueue<Pair<ShipId, InvForceAtPos>>()
    private val rotPosForces = ConcurrentLinkedQueue<Pair<ShipId, InvForceAtPos>>()

    private val collisionChange = ConcurrentLinkedQueue<Tuple4<ShipId, ShipId, Boolean, CompletableFuture<Boolean>>>()

    private val joints = ConcurrentLinkedQueue<Tuple3<VSJoint, ((VSJoint, PhysLevel) -> Boolean)?, CompletableFuture<VSJointId?>>>()
    private val updatedJoints = ConcurrentLinkedQueue<Tuple3<VSJointId, VSJoint, CompletableFuture<Boolean>>>()
    private val deletedJoints = ConcurrentLinkedQueue<Pair<VSJointId, CompletableFuture<Boolean>>>()

    private val toBeStatic = ConcurrentLinkedQueue<Pair<ShipId, Boolean>>()

    private data class InvForceAtPos(val force: Vector3dc, val pos: Vector3dc)

    fun physTick(level: PhysLevel, delta: Double) {
        level as PhysLevelCore

        invForces   .pollUntilEmpty { (id, force) -> level.getShipById(id)?.applyInvariantForce(force) }
        invTorques  .pollUntilEmpty { (id, force) -> level.getShipById(id)?.applyInvariantTorque(force) }
        rotForces   .pollUntilEmpty { (id, force) -> level.getShipById(id)?.applyRotDependentForce(force) }
        rotTorques  .pollUntilEmpty { (id, force) -> level.getShipById(id)?.applyRotDependentTorque(force) }
        invPosForces.pollUntilEmpty { (id, fPos ) -> level.getShipById(id)?.applyInvariantForceToPos(fPos.force, fPos.pos) }
        rotPosForces.pollUntilEmpty { (id, fPos ) -> level.getShipById(id)?.applyInvariantForceToPos(fPos.force, fPos.pos) }

        collisionChange.pollUntilEmpty { (id1, id2, change, future) ->
            future.complete(
                when (change) {
                    true  -> level.enableCollisionBetween(id1, id2)
                    false -> level.disableCollisionBetween(id1, id2)
                }
            )
        }

        val rePoll = mutableListOf<Tuple3<VSJoint, ((VSJoint, PhysLevel) -> Boolean)?, CompletableFuture<VSJointId?>>>()
        joints.pollUntilEmpty { (joint, predicate, future) ->
            if (predicate != null && predicate.invoke(joint, level) == false) {
                rePoll.add(Tuple.of(joint, predicate, future))
                return@pollUntilEmpty
            }

            future.complete(level.addJoint(joint))
        }
        joints.addAll(rePoll)

        updatedJoints.pollUntilEmpty { (id, joint, future) -> future.complete(level.updateJoint(id, joint)) }
        deletedJoints.pollUntilEmpty { (id, future) -> future.complete(level.removeJoint(id)) }

        toBeStatic.pollUntilEmpty { pair -> level.getShipById(pair.first)?.isStatic = pair.second }
    }

    fun applyInvariantForce(ship: ShipId, force: Vector3dc) { invForces.add(ship to force) }
    fun applyInvariantTorque(ship: ShipId, torque: Vector3dc) { invTorques.add(ship to torque) }
    fun applyRotDependentForce(ship: ShipId, force: Vector3dc) { rotForces.add(ship to force) }
    fun applyRotDependentTorque(ship: ShipId, torque: Vector3dc) { rotTorques.add(ship to torque) }
    fun applyInvariantForceToPos(ship: ShipId, force: Vector3dc, pos: Vector3dc) { invPosForces.add(ship to InvForceAtPos(force, pos)) }
    fun applyRotDependentForceToPos(ship: ShipId, force: Vector3dc, pos: Vector3dc) { rotPosForces.add(ship to InvForceAtPos(force, pos)) }

    fun setStatic(ship: ShipId, b: Boolean) { toBeStatic.add(ship to b) }

    fun addJoint(joint: VSJoint, checkValid: ((VSJoint, PhysLevel) -> Boolean)? = null): CompletableFuture<VSJointId?> = CompletableFuture<VSJointId?>().also { joints.add(Tuple.of(joint, checkValid, it)) }
    fun updateJoint(id: VSJointId, joint: VSJoint): CompletableFuture<Boolean> = CompletableFuture<Boolean>().also { updatedJoints.add(Tuple.of(id, joint, it)) }
    fun removeJoint(id: VSJointId): CompletableFuture<Boolean> = CompletableFuture<Boolean>().also { deletedJoints.add(id to it) }
    fun disableCollisionBetweenBodies(id1: ShipId, id2: ShipId): CompletableFuture<Boolean> = CompletableFuture<Boolean>().also { collisionChange.add(Tuple.of(id1, id2, false, it)) }
    fun enableCollisionBetweenBodies (id1: ShipId, id2: ShipId): CompletableFuture<Boolean> = CompletableFuture<Boolean>().also { collisionChange.add(Tuple.of(id1, id2, true, it)) }
}