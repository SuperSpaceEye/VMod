package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.PhysEntityBlockRenderer
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
import org.joml.Vector3i
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.bodies.VsBodyCreateData
import org.valkyrienskies.core.api.bodies.properties.BodyInertia
import org.valkyrienskies.core.api.bodies.shape.VoxelType
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.physics.PhysicsEntityData
import org.valkyrienskies.core.internal.physics.VSSphereCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.core.internal.game.StandaloneBodyCreateData
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.awt.Color

class FunnyMode: ExtendableToolgunMode(), SimpleHUD {
    override val itemName get() = makeFake("Funny")
    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("Funny Mode")
    }

    private fun sphereInertiaTensor(mass: Double, radius: Double): Matrix3d {
        return Matrix3d().identity().scale(mass)
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = level.shipObjectWorld.loadedShips.getById(raycastResult.shipId) ?: return
        val aabb = ship.shipAABB ?: return
        val radius = 0.5
        val shipRot = ship.transform.rotation

        val fakeBlocks = mutableListOf<Pair<Vector3d, BlockState>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}

            fakeBlocks.add(posShipToWorld(ship, Vector3d(x, y, z) + 0.5) to state)
        } } }


        level.shipObjectWorld.deleteShip(ship)
        fakeBlocks.forEach { (pos, state) ->
            val (mass, type) = BlockStateInfo.get(state)!!
            val offset = 0.25
            val body = level.shipObjectWorld.createBody(VsBodyCreateData(
                level.dimensionId,
                vsCore.newShipInertiaData(JVector3d(), mass, sphereInertiaTensor(mass, radius)),
                vsCore.newBodyKinematics(JVector3d(), JVector3d(), (pos-offset).toJomlVector3d(), shipRot, JVector3d(1.0, 1.0, 1.0), JVector3d(0.0, 0.0, 0.0)),
//                vsCore.newSphereBodyShape(0.5),
                vsCore.newBoxBodyShape(JVector3d(1.0, 1.0, 1.0)),
//                vsCore.newCompoundBodyShape(listOf(
//                    vsCore.newCompoundBodyShapeChild(vsCore.newVoxelBodyShape(
//                        Vector3i(0, 0, 0),
//                        Vector3i(1, 1, 1),
//                        AABBi(0, 0, 0, 1, 1, 1)
//                    ))
//                )),

                staticFrictionCoefficient = 1.0,
                dynamicFrictionCoefficient = 1.0,
                restitutionCoefficient = 1.0
            ))

//            val segmentId = 0
//
//            val update = vsCore.newSparseVoxelUpdateBuilder(0, 0, 0)
//                .apply { addBlock(0, 0, 0, type as VoxelType, mass) }
//                .build()
//
//            body.applyVoxelSegmentUpdate(segmentId, update)

            val newId = body.id

            RenderingData.server.addRenderer(listOf(newId), PhysEntityBlockRenderer(
                newId, state, Color(255, 255, 255, 255), true
            ))

//            entity.physicsListeners.add(object : ShipPhysicsListener {
//                override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
//                    physShip as PhysShipImpl
//
//                    val force = -Vector3d(physShip.velocity) * physShip.mass * 0.05
//                    val omega = -Vector3d(physShip.angularVelocity) * physShip.mass * 0.05
//
//                    physShip.applyInvariantForce(force.toJomlVector3d())
//                    physShip.applyInvariantTorque(omega.toJomlVector3d())
//                }
//            })
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