package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.valkyrien_ship_schematics.ELOG
import net.spaceeye.vmod.compat.vsBackwardsCompat.mass
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.PhysEntityBlockRenderer
import net.spaceeye.vmod.toolgun.modes.EHUDBuilder
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData
import org.valkyrienskies.core.apigame.physics.VSSphereCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class FunnyMode: ExtendableToolgunMode(), SimpleHUD {
    override val itemName get() = makeFake("Funny")
    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("Funny Mode")
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

        val fakeBlocks = mutableListOf<Pair<Vector3d, BlockState>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}

            fakeBlocks.add(posShipToWorld(ship, Vector3d(x, y, z)) to state)
        } } }

        ELOG("${fakeBlocks.size}")

        level.shipObjectWorld.deleteShip(ship)
        fakeBlocks.forEach { (pos, state) ->
            val newId = level.shipObjectWorld.allocateShipId(level.dimensionId)
            val (mass, _) = BlockStateInfo.get(state)!!

            RenderingData.server.addRenderer(listOf(newId), PhysEntityBlockRenderer(
                newId, state, Color(255, 255, 255, 255), true
            ))
            val entity = level.shipObjectWorld.createPhysicsEntity(PhysicsEntityData(
                newId,
                ShipTransformImpl((pos + 0.5).toJomlVector3d(), JVector3d(), Quaterniond(), JVector3d(1.0, 1.0, 1.0)),
                ShipInertiaDataImpl(JVector3d(), mass, sphereInertiaTensor(mass, radius)),
                JVector3d(), JVector3d(),
                VSSphereCollisionShapeData(radius),
            ), level.dimensionId)

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
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(FunnyMode::class) {
                it.addExtension {
                    BasicConnectionExtension<FunnyMode>("funny_mode"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}