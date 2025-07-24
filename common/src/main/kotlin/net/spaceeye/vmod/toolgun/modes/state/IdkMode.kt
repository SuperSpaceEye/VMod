package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.PhysEntityBlockRenderer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.gui.IdkModeGUI
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple4
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.gtpa
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.apigame.joints.VSD6Joint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData
import org.valkyrienskies.core.apigame.physics.VSSphereCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.sqrt

class IdkMode: ExtendableToolgunMode(), IdkModeGUI, AutoSerializable {
    @JsonIgnore private var i = 0;

    var radius: Double by get(i++, 0.5)
    var stiffness: Float by get(i++, 1e7f) { ServerLimits.instance.stiffness.get(it) }
    var damping: Float by get(i++, -1f) { ServerLimits.instance.damping.get(it) }
    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }

    private fun sphereInertiaTensor(mass: Double, radius: Double): Matrix3d {
        val mat = Matrix3d()

        mat.m00 = (2.0/5.0) * mass * radius * radius
        mat.m11 = (2.0/5.0) * mass * radius * radius
        mat.m22 = (2.0/5.0) * mass * radius * radius

        return mat
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = level.shipObjectWorld.loadedShips.getById(raycastResult.shipId) ?: return
        val aabb = ship.shipAABB ?: return
        val shipRot = ship.transform.rotation

        val blocks = mutableMapOf<BlockPos, Tuple4<BlockState, Vector3d, MutableList<BlockPos>, Long>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}

            val neighbours = mutableListOf<BlockPos>()
            for (x1 in x-1..x+1) {
            for (z1 in z-1..z+1) {
            for (y1 in y-1..y+1) {
                val nbpos = BlockPos(x1, y1, z1)
                if (level.getBlockState(nbpos).isAir) continue
                neighbours.add(nbpos)
            }}}

            blocks[bpos] = Tuple.of(state, posShipToWorld(ship, Vector3d(x, y, z) + 0.5), neighbours, -1)
        } } }

        level.shipObjectWorld.deleteShip(ship)
        blocks.forEach { (_, item) ->
            val (state, pos, _, _) = item

            val newId = level.shipObjectWorld.allocateShipId(level.dimensionId)
            val (mass, _) = BlockStateInfo.get(state)!!

            RenderingData.server.addRenderer(listOf(newId), PhysEntityBlockRenderer(
                newId, state, Color(255, 255, 255, 255), true
            ))
            val entity = level.shipObjectWorld.createPhysicsEntity(PhysicsEntityData(
                newId,
                ShipTransformImpl.create(pos.toJomlVector3d(), JVector3d(), shipRot, JVector3d(1.0, 1.0, 1.0)),
                ShipInertiaDataImpl(JVector3d(), mass, sphereInertiaTensor(mass, radius)),
                JVector3d(), JVector3d(),
                VSSphereCollisionShapeData(radius),
            ), level.dimensionId)

            item.i4 = newId

            entity.physicsListeners.add(object : ShipPhysicsListener {
                override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
                    physShip as PhysShipImpl

                    val force = -Vector3d(physShip.velocity) * physShip.mass * 0.05
                    val omega = -Vector3d(physShip.angularVelocity) * physShip.mass * 0.05

                    physShip.applyInvariantForce(force.toJomlVector3d())
                    physShip.applyInvariantTorque(omega.toJomlVector3d())
                }
            })
        }

        val stiffness = if (stiffness <= 0f) Float.MAX_VALUE else stiffness
        val damping = if (damping <= 0f) Float.MAX_VALUE else damping
        val maxForceTorque = if (maxForce <= 0f) VSJointMaxForceTorque(Float.MAX_VALUE, Float.MAX_VALUE) else VSJointMaxForceTorque(maxForce, maxForce)

        val zero = JVector3d(0.0, 0.0, 0.0)
        val rotation = Quaterniond()

        val twoRoot = sqrt(2.0)
        val threeRoot = sqrt(3.0)

        for ((mbpos, item) in blocks) {
            val (_, _, neighborBpos, mId) = item
            for (sbpos in neighborBpos) {
                val (_, _, _, sId) = blocks[sbpos]!!
                if (mId == sId) continue
                val bDir = sbpos.subtract(mbpos)

                val distance = when (abs(bDir.x) + abs(bDir.y) + abs(bDir.z)) {
                    0 -> continue
                    1 -> 1.0
                    2 -> twoRoot
                    3 -> threeRoot
                    else -> throw AssertionError("Impossible")
                }.toFloat()

                if (abs(bDir.x) + abs(bDir.y) + abs(bDir.z) > 1) continue

                val dir = (Vector3d(bDir) * (radius * 2.0 * distance))
                val nDir = -dir

                //TODO make constructors for fixed orientation, etc
                level.gtpa.addJoint(
                    VSD6Joint(
                        mId, VSJointPose(zero, getHingeRotation( dir)),
                        sId, VSJointPose(zero, getHingeRotation(nDir)),
                        motions = EnumMap(mapOf(
                             Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.LIMITED),

                            Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                            Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                            Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                        )),
                        linearLimits = EnumMap(mapOf(
                            Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(distance, distance, stiffness = stiffness, damping = damping)))
                        ),
                        maxForceTorque = maxForceTorque
                    )
                ) {j, level -> level.getShipById(j.shipId0!!) != null && level.getShipById(j.shipId1!!) != null}

                level.gtpa.addJoint(
                    VSD6Joint(
                        mId, VSJointPose(zero, rotation.invert(Quaterniond())),
                        sId, VSJointPose(zero, rotation.invert(Quaterniond())),
                        motions = EnumMap(mapOf(
                            Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                            Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                            Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                        )),
                        twistLimit = VSD6Joint.AngularLimitPair(0f, 0f, stiffness = stiffness, damping = damping),
                        swingLimit = VSD6Joint.LimitCone(0f, 0f, stiffness = stiffness, damping = damping),
                        pyramidSwingLimit = VSD6Joint.LimitPyramid(0f, 0f, 0f, 0f, stiffness = stiffness, damping = damping),
                        maxForceTorque = maxForceTorque
                    )
                ) {j, level -> level.getShipById(j.shipId0!!) != null && level.getShipById(j.shipId1!!) != null}
            }
        }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(IdkMode::class) {
                it.addExtension {
                    BasicConnectionExtension<IdkMode>("idk_mode"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}