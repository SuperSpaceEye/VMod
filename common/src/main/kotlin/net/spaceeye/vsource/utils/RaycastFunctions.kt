package net.spaceeye.vsource.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.abs
import kotlin.math.max

object RaycastFunctions {
    const val eps = 1e-200
    class RayIntersectBoxResult(@JvmField var intersects: Boolean, @JvmField var tToIn: Double, @JvmField var tToOut: Double)

    class RaycastResult(
        @JvmField var state: BlockState,
        @JvmField var origin: Vector3d,
        @JvmField var lookVec: Vector3d,
        @JvmField var worldHitPos: Vector3d, // if in shipyard, will transform to world pos
        @JvmField var globalHitPos: Vector3d, // if in shipyard, will not transform to world pos
        @JvmField var blockPosition: BlockPos,
        @JvmField var hitNormal: Vector3d,
    )

    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    //first t is time to in collision point, second t is time to out collision point
    @JvmStatic
    fun rayIntersectsBox(box: AABB, ray_origin: Vector3d, d: Vector3d): RayIntersectBoxResult {
        val t1: Double = (box.minX - ray_origin.x) * d.x
        val t2: Double = (box.maxX - ray_origin.x) * d.x
        val t3: Double = (box.minY - ray_origin.y) * d.y
        val t4: Double = (box.maxY - ray_origin.y) * d.y
        val t5: Double = (box.minZ - ray_origin.z) * d.z
        val t6: Double = (box.maxZ - ray_origin.z) * d.z

        val tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6))
        val tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return RayIntersectBoxResult(false, tmax, tmin)}
        return RayIntersectBoxResult(true, tmin, tmax)
    }

    @JvmStatic
    fun calculateNormal(start: Vector3d, unitD: Vector3d, raycastResult: RayIntersectBoxResult, dist: Double): Vector3d {
        val box_hit = (start + unitD * raycastResult.tToIn * dist) - (start + unitD * raycastResult.tToIn * dist).sfloor() - 0.5
        val normal = box_hit / max(max(abs(box_hit.x), abs(box_hit.y)), abs(box_hit.z))

        //mot sure why i'm doing abs but it seems to work
        normal.sabs().sclamp(0.0, 1.0).smul(1.0000001).sfloor().snormalize()
        return normal
    }

    fun raycast(level: Level, player: Player, maxDistance: Double = 100.0): RaycastResult {
        var unitLookVec = Vector3d(player.lookAngle).snormalize()
        var origin = Vector3d(player.eyePosition)

        var d = (unitLookVec + eps).srdiv(1.0)
        val rayDistance = unitLookVec.dist()

        val clipResult = level.clip(
            ClipContext(
                player.eyePosition,
                (origin + unitLookVec * maxDistance).toMCVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
        )

        val state = level.getBlockState(clipResult.blockPos)
        if (state.isAir) {
            return RaycastResult(state, origin, unitLookVec, origin + unitLookVec * rayDistance, origin + unitLookVec * rayDistance, clipResult.blockPos, Vector3d())
        }

        val ship = level.getShipManagingPos(clipResult.blockPos)

        val bpos = Vector3d(clipResult.blockPos)

        if (ship != null) {
            origin = posWorldToShip(ship, origin)
            d = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(d.sdiv(1.0).toJomlVector3d(), JVector3d())).sdiv(1.0)
        }

        val result = rayIntersectsBox(AABB(bpos.x, bpos.y, bpos.z, bpos.x+1, bpos.y+1, bpos.z+1), origin, d)
        var normal = calculateNormal(origin, unitLookVec, result, rayDistance)

        if (ship != null) {
            origin = posShipToWorld(ship, origin)
            d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(d.sdiv(1.0).toJomlVector3d(), JVector3d())).sdiv(1.0)
        }

        var worldHitPos: Vector3d = origin + unitLookVec * (rayDistance * result.tToIn)
        var shipHitPos = Vector3d(worldHitPos)

        if (ship != null) {
            normal = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(normal.toJomlVector3d(), JVector3d()))

            shipHitPos = posWorldToShip(ship, worldHitPos)
        }

        return RaycastResult(state, origin, unitLookVec, worldHitPos, shipHitPos, clipResult.blockPos, normal)
    }
}