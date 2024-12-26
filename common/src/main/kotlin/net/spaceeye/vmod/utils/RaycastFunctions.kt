package net.spaceeye.vmod.utils

import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.vanillaClip
import kotlin.math.abs
import kotlin.math.max

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.spaceeye.vmod.utils.vs.*
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import java.util.function.BiFunction
import java.util.function.Function

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
        @JvmField var globalNormalDirection: Vector3d?,
        @JvmField var ship: Ship?,
        @JvmField var shipId: ShipId
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

    inline fun renderRaycast(level: Level, source: Source, maxDistance: Double = 100.0, skipShipId: ShipId? = null,
                noinline transformShipToWorld: (ship: Ship, dir: Vector3d) -> Vector3d = {ship, dir -> transformDirectionShipToWorldRenderNoScaling(ship as ClientShip, dir) },
                noinline transformWorldToShip: (ship: Ship, dir: Vector3d) -> Vector3d = {ship, dir -> transformDirectionWorldToShipRenderNoScaling(ship as ClientShip, dir) },
                noinline posShipToWorld: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = {ship, pos, transform -> posShipToWorldRender(ship as ClientShip, pos, transform) },
                noinline posWorldToShip: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = {ship, pos, transform -> posWorldToShipRender(ship as ClientShip, pos, transform) }
    ): RaycastResult = raycast(level, source, maxDistance, if (skipShipId != null) setOf(skipShipId) else null, transformShipToWorld, transformWorldToShip, posShipToWorld, posWorldToShip)


    inline fun renderRaycast(level: Level, source: Source, maxDistance: Double = 100.0, skipShipId: Set<ShipId>?,
                noinline transformShipToWorld: (ship: Ship, dir: Vector3d) -> Vector3d = {ship, dir -> transformDirectionShipToWorldRenderNoScaling(ship as ClientShip, dir) },
                noinline transformWorldToShip: (ship: Ship, dir: Vector3d) -> Vector3d = {ship, dir -> transformDirectionWorldToShipRenderNoScaling(ship as ClientShip, dir) },
                noinline posShipToWorld: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = {ship, pos, transform -> posShipToWorldRender(ship as ClientShip, pos, transform) },
                noinline posWorldToShip: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = {ship, pos, transform -> posWorldToShipRender(ship as ClientShip, pos, transform) }
    ): RaycastResult = raycast(level, source, maxDistance, skipShipId, transformShipToWorld, transformWorldToShip, posShipToWorld, posWorldToShip)

    inline fun raycast(level: Level, source: Source, maxDistance: Double = 100.0, skipShipId: ShipId? = null,
                noinline transformShipToWorld: (ship: Ship, dir: Vector3d) -> Vector3d = ::transformDirectionShipToWorldNoScaling,
                noinline transformWorldToShip: (ship: Ship, dir: Vector3d) -> Vector3d = ::transformDirectionWorldToShipNoScaling,
                noinline posShipToWorld: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = ::posShipToWorld,
                noinline posWorldToShip: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = ::posWorldToShip
    ): RaycastResult = raycast(level, source, maxDistance, if (skipShipId != null) setOf(skipShipId) else null, transformShipToWorld, transformWorldToShip, posShipToWorld, posWorldToShip)


    fun raycast(level: Level, source: Source, maxDistance: Double = 100.0, skipShipId: Set<ShipId>?,
                transformShipToWorld: (ship: Ship, dir: Vector3d) -> Vector3d = ::transformDirectionShipToWorldNoScaling,
                transformWorldToShip: (ship: Ship, dir: Vector3d) -> Vector3d = ::transformDirectionWorldToShipNoScaling,
                posShipToWorld: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = ::posShipToWorld,
                posWorldToShip: (ship: Ship?, pos: Vector3d, transform: ShipTransform?) -> Vector3d = ::posWorldToShip
    ): RaycastResult {
        var unitLookVec = Vector3d(source.dir).snormalize()
        val clipResult = level.clipIncludeShips(
            ClipContext(
                source.origin.toMCVec3(),
                (source.origin + unitLookVec * maxDistance).toMCVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ), skipShipId
        )

        val state = level.getBlockState(clipResult.blockPos)
        if (state.isAir) { return RaycastResult(state, source.origin, unitLookVec, clipResult.blockPos, null, null, null, null, null, null, null, null, -1) }

        val ship = level.getShipManagingPos(clipResult.blockPos)

        val bpos = Vector3d(clipResult.blockPos)

        if (ship != null) {
            source.origin = posWorldToShip(ship, source.origin, null)
            unitLookVec = transformWorldToShip(ship, unitLookVec)
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
            normalDirection = transformShipToWorld(ship, normalDirection)
            unitLookVec = transformShipToWorld(ship, unitLookVec)
            source.origin = posShipToWorld(ship, source.origin, null)
            worldHitPos = posShipToWorld(ship, globalHitPos, null)
            worldCenteredHitPos = posShipToWorld(ship, globalCenteredHitPos, null)
        }

        return RaycastResult(state, source.origin, unitLookVec, clipResult.blockPos, worldHitPos, globalHitPos, worldCenteredHitPos, globalCenteredHitPos, normal, normalDirection, globalNormalDirection, ship, ship?.id ?: if (level is ServerLevel) level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!! else -1)
    }

    fun Level.clipIncludeShips(
        ctx: ClipContext, skipShips: Set<ShipId>? = null
    ): BlockHitResult {
        val vanillaHit = vanillaClip(ctx)

        var closestHit = vanillaHit
        val closestHitPos = vanillaHit.location
        var closestHitDist = closestHitPos.distanceToSqr(ctx.from)

        val clipAABB: AABBdc = AABBd(ctx.from.toJOML(), ctx.to.toJOML()).correctBounds()

        // Iterate every ship, find do the raycast in ship space,
        // choose the raycast with the lowest distance to the start position.
        for (ship in shipObjectWorld.loadedShips.getIntersecting(clipAABB)) {
            // Skip skipShips
            if (skipShips != null && skipShips.contains(ship.id)) { continue }
            val worldToShip = (ship as? ClientShip)?.renderTransform?.worldToShip ?: ship.worldToShip
            val shipToWorld = (ship as? ClientShip)?.renderTransform?.shipToWorld ?: ship.shipToWorld
            val shipStart = worldToShip.transformPosition(ctx.from.toJOML()).toMinecraft()
            val shipEnd = worldToShip.transformPosition(ctx.to.toJOML()).toMinecraft()

            val shipHit = clip(ctx, shipStart, shipEnd)
            val shipHitPos = shipToWorld.transformPosition(shipHit.location.toJOML()).toMinecraft()
            val shipHitDist = shipHitPos.distanceToSqr(ctx.from)

            if (shipHitDist < closestHitDist && shipHit.type != HitResult.Type.MISS) {
                closestHit = shipHit
                closestHitDist = shipHitDist
            }
        }

        return closestHit
    }


    private fun Level.clip(context: ClipContext, realStart: Vec3, realEnd: Vec3): BlockHitResult {
        return clip(
            realStart, realEnd, context,
            { raycastContext: ClipContext, blockPos: BlockPos? ->
                val blockState: BlockState = getBlockState(blockPos!!)
                val fluidState: FluidState = getFluidState(blockPos)
                val vec3d = realStart
                val vec3d2 = realEnd
                val voxelShape = raycastContext.getBlockShape(blockState, this, blockPos)
                val blockHitResult: BlockHitResult? =
                    clipWithInteractionOverride(vec3d, vec3d2, blockPos, voxelShape, blockState)
                val voxelShape2 = raycastContext.getFluidShape(fluidState, this, blockPos)
                val blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, blockPos)
                val d = if (blockHitResult == null) Double.MAX_VALUE else realStart.distanceToSqr(blockHitResult.location)
                val e = if (blockHitResult2 == null) Double.MAX_VALUE else realEnd.distanceToSqr(blockHitResult2.location)
                if (d <= e) blockHitResult else blockHitResult2
            }
        ) { raycastContext: ClipContext ->
            val vec3d = realStart.subtract(realEnd)
            BlockHitResult.miss(realEnd, Direction.getNearest(vec3d.x, vec3d.y, vec3d.z), Vector3d(realEnd).toBlockPos())
        } as BlockHitResult
    }

    private fun <T> clip(
        realStart: Vec3,
        realEnd: Vec3,
        raycastContext: ClipContext,
        context: BiFunction<ClipContext, BlockPos?, T>,
        blockRaycaster: Function<ClipContext, T>
    ): T {
        val vec3d = realStart
        val vec3d2 = realEnd
        return if (vec3d == vec3d2) {
            blockRaycaster.apply(raycastContext)
        } else {
            val d = Mth.lerp(-1.0E-7, vec3d2.x, vec3d.x)
            val e = Mth.lerp(-1.0E-7, vec3d2.y, vec3d.y)
            val f = Mth.lerp(-1.0E-7, vec3d2.z, vec3d.z)
            val g = Mth.lerp(-1.0E-7, vec3d.x, vec3d2.x)
            val h = Mth.lerp(-1.0E-7, vec3d.y, vec3d2.y)
            val i = Mth.lerp(-1.0E-7, vec3d.z, vec3d2.z)
            var j = Mth.floor(g)
            var k = Mth.floor(h)
            var l = Mth.floor(i)
            val mutable = BlockPos.MutableBlockPos(j, k, l)
            val `object`: T? = context.apply(raycastContext, mutable)
            if (`object` != null) {
                `object`
            } else {
                val m = d - g
                val n = e - h
                val o = f - i
                val p = Mth.sign(m)
                val q = Mth.sign(n)
                val r = Mth.sign(o)
                val s = if (p == 0) Double.MAX_VALUE else p.toDouble() / m
                val t = if (q == 0) Double.MAX_VALUE else q.toDouble() / n
                val u = if (r == 0) Double.MAX_VALUE else r.toDouble() / o
                var v = s * if (p > 0) 1.0 - Mth.frac(g) else Mth.frac(g)
                var w = t * if (q > 0) 1.0 - Mth.frac(h) else Mth.frac(h)
                var x = u * if (r > 0) 1.0 - Mth.frac(i) else Mth.frac(i)
                var object2: T?
                do {
                    if (v > 1.0 && w > 1.0 && x > 1.0) {
                        return blockRaycaster.apply(raycastContext)
                    }
                    if (v < w) {
                        if (v < x) {
                            j += p
                            v += s
                        } else {
                            l += r
                            x += u
                        }
                    } else if (w < x) {
                        k += q
                        w += t
                    } else {
                        l += r
                        x += u
                    }
                    object2 = context.apply(raycastContext, mutable.set(j, k, l))
                } while (object2 == null)
                object2
            }
        }
    }
}