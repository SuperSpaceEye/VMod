package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.compat.vsBackwardsCompat.mass
import net.spaceeye.vmod.compat.vsBackwardsCompat.rotation
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.PhysEntityBlockRenderer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple4
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData
import org.valkyrienskies.core.apigame.physics.VSSphereCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sqrt

class IdkMode: ExtendableToolgunMode(), SimpleHUD {
    override val itemName: TranslatableComponent get() = makeFake("IDK")
    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("IDK Mode")
    }

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
        val radius = 0.5
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
                ShipTransformImpl(pos.toJomlVector3d(), JVector3d(), shipRot, JVector3d(1.0, 1.0, 1.0)),
                ShipInertiaDataImpl(JVector3d(), mass, sphereInertiaTensor(mass, radius)),
                JVector3d(), JVector3d(),
                VSSphereCollisionShapeData(radius),
            ), level.dimensionId)

            item.i4 = newId

            entity.forceInducers.add(object : ShipForcesInducer {
                override fun applyForces(physShip: PhysShip) {
                    physShip as PhysShipImpl

                    val force = -Vector3d(physShip.poseVel.vel) * physShip.mass * 0.05
                    val omega = -Vector3d(physShip.poseVel.omega) * physShip.mass * 0.05

                    physShip.applyInvariantForce(force.toJomlVector3d())
                    physShip.applyInvariantTorque(omega.toJomlVector3d())
                }
            })
        }

        val compliance = 1e-7
        val maxForce   = 1e+300

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
                }

                if (abs(bDir.x) + abs(bDir.y) + abs(bDir.z) > 1) continue

                val dir = (Vector3d(bDir) * (radius * 2.0 * distance)).toJomlVector3d()
                val nDir = dir.negate(JVector3d())

                level.shipObjectWorld.createNewConstraint(VSAttachmentConstraint(mId, sId, compliance, zero, nDir, maxForce, 0.0))
                level.shipObjectWorld.createNewConstraint(VSAttachmentConstraint(mId, sId, compliance, dir , zero, maxForce, 0.0))
                level.shipObjectWorld.createNewConstraint(VSFixedOrientationConstraint(mId, sId, compliance, rotation, rotation, maxForce))
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