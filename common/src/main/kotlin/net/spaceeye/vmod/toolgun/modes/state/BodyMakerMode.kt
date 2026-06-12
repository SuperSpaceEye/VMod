package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.bodies.VsBodyCreateData
import org.valkyrienskies.core.api.bodies.shape.VoxelType
import org.valkyrienskies.core.api.bodies.shape.VoxelUpdate
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.forEach
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore

data class Voxel(val x: Int, val y: Int, val z: Int, val mass: Double)

object VoxelInertiaJOML {
    /** Convert integer grid position to the physical center of the cube. */
    private fun Voxel.center(): Vector3d =
        Vector3d(x + 0.5, y + 0.5, z + 0.5)

    fun centerOfMass(voxels: List<Voxel>): Vector3d {
        val acc = Vector3d()
        var total = 0.0
        for (v in voxels) {
            val c = v.center()
            acc.add(c.x * v.mass, c.y * v.mass, c.z * v.mass)
            total += v.mass
        }
        require(total > 0.0)
        return acc.div(total)
    }

    fun totalMass(voxels: List<Voxel>): Double = voxels.sumOf { it.mass }

    /** Inertia tensor about the world origin (0,0,0). */
    fun inertiaAboutOrigin(voxels: List<Voxel>): Matrix3d {
        var ixx = 0.0; var iyy = 0.0; var izz = 0.0
        var ixy = 0.0; var ixz = 0.0; var iyz = 0.0

        for (v in voxels) {
            val m = v.mass
            val x = v.center().x
            val y = v.center().y
            val z = v.center().z

            ixx += m * (y * y + z * z)
            iyy += m * (x * x + z * z)
            izz += m * (x * x + y * y)
            ixy -= m * x * y
            ixz -= m * x * z
            iyz -= m * y * z
        }
        return Matrix3d(ixx, ixy, ixz, ixy, iyy, iyz, ixz, iyz, izz)
    }

    /** Inertia tensor computed directly about the center of mass. */
    fun inertiaAboutCOM(voxels: List<Voxel>): Matrix3d {
        val com = centerOfMass(voxels)

        var ixx = 0.0; var iyy = 0.0; var izz = 0.0
        var ixy = 0.0; var ixz = 0.0; var iyz = 0.0

        for (v in voxels) {
            val m = v.mass
            val x = v.center().x - com.x
            val y = v.center().y - com.y
            val z = v.center().z - com.z

            ixx += m * (y * y + z * z)
            iyy += m * (x * x + z * z)
            izz += m * (x * x + y * y)
            ixy -= m * x * y
            ixz -= m * x * z
            iyz -= m * y * z
        }
        return Matrix3d(ixx, ixy, ixz, ixy, iyy, iyz, ixz, iyz, izz)
    }

    /**
     * Parallel axis theorem: shift an origin-referenced tensor to the COM.
     *
     * @param tensor Inertia about the current reference point.
     * @param mass   Total mass.
     * @param com    Vector from the current reference point to the COM.
     */
    fun shiftToCOM(tensor: Matrix3d, mass: Double, com: Vector3d): Matrix3d {
        val dx = com.x; val dy = com.y; val dz = com.z
        val d2 = dx * dx + dy * dy + dz * dz

        return Matrix3d(
            tensor.m00 - mass * (d2 - dx * dx),
            tensor.m01 + mass * dx * dy,
            tensor.m02 + mass * dx * dz,

            tensor.m10 + mass * dx * dy,
            tensor.m11 - mass * (d2 - dy * dy),
            tensor.m12 + mass * dy * dz,

            tensor.m20 + mass * dx * dz,
            tensor.m21 + mass * dy * dz,
            tensor.m22 - mass * (d2 - dz * dz)
        )
    }

    /**
     * If voxels are solid cubes of side [cubeSize], add the self-inertia
     * of each cube about its own center: (1/6)·M·s² to every diagonal.
     */
    fun withCubeExtent(tensor: Matrix3d, totalMass: Double, cubeSize: Double = 1.0): Matrix3d {
        val self = (1.0 / 6.0) * totalMass * cubeSize * cubeSize
        return Matrix3d(
            tensor.m00 + self, tensor.m01,       tensor.m02,
            tensor.m10,       tensor.m11 + self, tensor.m12,
            tensor.m20,       tensor.m21,       tensor.m22 + self
        )
    }

    /** Scalar moment of inertia about an arbitrary unit axis. */
    fun momentAboutAxis(tensor: Matrix3d, axis: Vector3d): Double {
        val nx = axis.x; val ny = axis.y; val nz = axis.z
        return nx * nx * tensor.m00 +
                ny * ny * tensor.m11 +
                nz * nz * tensor.m22 +
                2.0 * nx * ny * tensor.m01 +
                2.0 * nx * nz * tensor.m02 +
                2.0 * ny * nz * tensor.m12
    }
}

class BodyMakerMode: ExtendableToolgunMode(), SimpleHUD {
    override val itemName: Component = makeFake("Body Maker")

    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("LMB on a ship to bodify it")
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) {
        val ship = rr.ship as? ServerShip ?: return
        val aabb = ship.shipAABB ?: return

        val pos = ship.transform.position.get(Vector3d())
        val rot = ship.transform.rotation.get(Quaterniond())
        val scaling = ship.transform.scaling.get(Vector3d())

        val min = Vector3i(aabb.minX(), aabb.minY(), aabb.minZ())

        val minAABB = Vector3i(aabb.minX(), aabb.minY(), aabb.minZ()).sub(min)
        val maxAABB = Vector3i(aabb.maxX(), aabb.maxY(), aabb.maxZ()).sub(min)

        val shape = vsCore.newVoxelBodyShape(minAABB, maxAABB, AABBi(minAABB, maxAABB))

        val chunkData = mutableMapOf<Vector3i, MutableList<Pair<Vector3i,  Pair<Double, VsiBlockType>>>>()
        val voxels = mutableListOf<Voxel>()
        var mass = 0.0

        aabb.forEach { x, y, z ->
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) return@forEach

            val type = BlockStateInfo.get(state) ?: return@forEach

            val x = x - min.x
            val y = y - min.y
            val z = z - min.z

            voxels.add(Voxel(x, y, z, type.first))
            mass += type.first

            val cX = x shr 4
            val cY = y shr 4
            val cZ = z shr 4
            chunkData
                .getOrPut(Vector3i(cX, cY, cZ)) { mutableListOf() }
                .add(Vector3i(x - (cX shl 4), y - (cY shl 4), z - (cZ shl 4)) to type)
        }

        level.shipObjectWorld.deleteShip(ship)

        val updates = mutableListOf<VoxelUpdate>()

        for ((chunk, data) in chunkData) {
            val update = vsCore.newSparseVoxelUpdateBuilder(chunk.x, chunk.y, chunk.z)
            for ((pos, data) in data) {
                update.addBlock(pos.x, pos.y, pos.z, data.second as VoxelType, data.first)
            }
            updates.add(update.build())
        }

        val COM = VoxelInertiaJOML.centerOfMass(voxels)
        val MOI = VoxelInertiaJOML.inertiaAboutCOM(voxels)

        val body = level.shipObjectWorld.createBody(VsBodyCreateData(
            level.dimensionId,
            vsCore.newShipInertiaData(COM, mass, MOI),
            vsCore.newBodyKinematics(JVector3d(), JVector3d(), pos, rot, scaling, COM),
            vsCore.newCompoundBodyShape(listOf(vsCore.newCompoundBodyShapeChild(shape))),
        ))

        updates.forEach { body.applyVoxelSegmentUpdate(0, it) }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(BodyMakerMode::class) {
                it.addExtension {
                    BasicConnectionExtension<BodyMakerMode>("body_maker_mode",
                        leftFunction = {inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr)})
                }
            }
        }
    }
}