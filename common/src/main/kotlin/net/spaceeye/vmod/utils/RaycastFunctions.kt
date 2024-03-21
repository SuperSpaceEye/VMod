package net.spaceeye.vmod.utils

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.world.vanillaClip
import kotlin.math.abs
import kotlin.math.max

object RaycastFunctions {
    const val eps = 1e-200
    class RayIntersectBoxResult(@JvmField var intersects: Boolean, @JvmField var tToIn: Double, @JvmField var tToOut: Double)

    data class Source(var dir: Vector3d, var origin: Vector3d)

    class RaycastResult(
        @JvmField var state: BlockState,
        @JvmField var origin: Vector3d,
        @JvmField var lookVec: Vector3d,
        @JvmField var blockPosition: BlockPos,
        @JvmField var worldHitPos: Vector3d?, // if in shipyard, will transform to world pos
        @JvmField var globalHitPos: Vector3d?, // if in shipyard, will not transform to world pos
        @JvmField var worldCenteredHitPos: Vector3d?,
        @JvmField var globalCenteredHitPos: Vector3d?,
        @JvmField var hitNormal: Vector3d?,
        @JvmField var worldNormalDirection: Vector3d?,
        @JvmField var globalNormalDirection: Vector3d?
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

    fun raycast(level: Level, source: Source, maxDistance: Double = 100.0): RaycastResult {
        var unitLookVec = Vector3d(source.dir).snormalize()

        val clipResult = level.clip(
            ClipContext(
                source.origin.toMCVec3(),
                (source.origin + unitLookVec * maxDistance).toMCVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
        )

        val state = level.getBlockState(clipResult.blockPos)
        if (state.isAir) { return RaycastResult(state, source.origin, unitLookVec, clipResult.blockPos, null, null, null, null, null, null, null) }

        val ship = level.getShipManagingPos(clipResult.blockPos)

        val bpos = Vector3d(clipResult.blockPos)

        if (ship != null) {
            source.origin = posWorldToShip(ship, source.origin)
            unitLookVec = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(unitLookVec.toJomlVector3d(), JVector3d()))
        }

        val result = rayIntersectsBox(AABB(bpos.x, bpos.y, bpos.z, bpos.x+1, bpos.y+1, bpos.z+1), source.origin, (unitLookVec + eps).srdiv(1.0))
        val normal = calculateNormal(source.origin, unitLookVec, result, unitLookVec.dist())

        val globalHitPos: Vector3d = source.origin + unitLookVec * (unitLookVec.dist() * result.tToIn)
        var worldHitPos = Vector3d(globalHitPos)

        val diff = globalHitPos - globalHitPos.floor()
        val offset = Vector3d(0, 0, 0)
        when {
            normal.x > 0.5 -> offset.sadd(if (diff.x >= 0.5) {1.0} else {0.0}, 0.5, 0.5)
            normal.y > 0.5 -> offset.sadd(0.5, if (diff.y >= 0.5) {1.0} else {0.0}, 0.5)
            normal.z > 0.5 -> offset.sadd(0.5, 0.5, if (diff.z >= 0.5) {1.0} else {0.0})
        }
        val globalCenteredHitPos = globalHitPos.floor().sadd(offset.x, offset.y, offset.z)
        var worldCenteredHitPos = Vector3d(globalCenteredHitPos)

        var normalDirection = (globalCenteredHitPos - bpos - 0.5) * 2
        val globalNormalDirection = Vector3d(normalDirection)

        if (ship != null) {
            normalDirection = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(normalDirection.toJomlVector3d(), JVector3d()))
            unitLookVec = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(unitLookVec.toJomlVector3d(), JVector3d()))
            source.origin = posShipToWorld(ship, source.origin)
            worldHitPos = posShipToWorld(ship, globalHitPos)
            worldCenteredHitPos = posShipToWorld(ship, globalCenteredHitPos)
        }

        return RaycastResult(state, source.origin, unitLookVec, clipResult.blockPos, worldHitPos, globalHitPos, worldCenteredHitPos, globalCenteredHitPos, normal, normalDirection, globalNormalDirection)
    }

    fun raycastNoShips(level: Level, source: Source, maxDistance: Double = 100.0): RaycastResult {
        val unitLookVec = Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize()
        val origin = Vector3d(Minecraft.getInstance().player!!.eyePosition)

        val clipResult = level.vanillaClip(
            ClipContext(
                source.origin.toMCVec3(),
                (origin + unitLookVec * maxDistance).toMCVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
        )

        val state = level.getBlockState(clipResult.blockPos)
        val bpos = Vector3d(clipResult.blockPos)

        val result = rayIntersectsBox(AABB(bpos.x, bpos.y, bpos.z, bpos.x+1, bpos.y+1, bpos.z+1), origin, (unitLookVec + eps).srdiv(1.0))
        val normal = calculateNormal(origin, unitLookVec, result, unitLookVec.dist())

        val globalHitPos: Vector3d = origin + unitLookVec * (unitLookVec.dist() * result.tToIn)

        val diff = globalHitPos - globalHitPos.floor()
        val offset = Vector3d(0, 0, 0)
        when {
            normal.x > 0.5 -> offset.sadd(if (diff.x >= 0.5) {1.0} else {0.0}, 0.5, 0.5)
            normal.y > 0.5 -> offset.sadd(0.5, if (diff.y >= 0.5) {1.0} else {0.0}, 0.5)
            normal.z > 0.5 -> offset.sadd(0.5, 0.5, if (diff.z >= 0.5) {1.0} else {0.0})
        }
        val globalCenteredHitPos = globalHitPos.floor().sadd(offset.x, offset.y, offset.z)

        val normalDirection = (globalCenteredHitPos - bpos - 0.5) * 2
        val globalNormalDirection = Vector3d(normalDirection)

        return RaycastResult(state, origin, unitLookVec, clipResult.blockPos, globalHitPos, globalHitPos, globalCenteredHitPos, globalCenteredHitPos, normal, normalDirection, globalNormalDirection)
    }
}