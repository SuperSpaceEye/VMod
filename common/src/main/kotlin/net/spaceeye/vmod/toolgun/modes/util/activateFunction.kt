package net.spaceeye.vmod.toolgun.modes.util

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.createSpacedPoints
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

enum class PositionModes {
    NORMAL,
    CENTERED_ON_SIDE,
    CENTERED_IN_BLOCK,
    PRECISE_PLACEMENT,
}

inline fun getModePosition(mode: PositionModes, pos: RaycastFunctions.RaycastResult, precisePlacementSideNum: Int = 3): Vector3d {
    return when(mode) {
        PositionModes.NORMAL -> pos.globalHitPos!!
        PositionModes.CENTERED_ON_SIDE -> pos.globalCenteredHitPos!!
        PositionModes.CENTERED_IN_BLOCK ->  Vector3d(pos.blockPosition) + 0.5
        PositionModes.PRECISE_PLACEMENT -> calculatePrecise(pos, precisePlacementSideNum)
    }
}

inline fun getModePositions(mode: PositionModes, prevPos: RaycastFunctions.RaycastResult, pos: RaycastFunctions.RaycastResult, precisePlacementSideNum: Int = 3): Pair<Vector3d, Vector3d> {
    return when(mode) {
        PositionModes.NORMAL -> Pair(prevPos.globalHitPos!!, pos.globalHitPos!!)
        PositionModes.CENTERED_ON_SIDE -> Pair(prevPos.globalCenteredHitPos!!, pos.globalCenteredHitPos!!)
        PositionModes.CENTERED_IN_BLOCK -> Pair(Vector3d(prevPos.blockPosition) + 0.5, Vector3d(pos.blockPosition) + 0.5)
        PositionModes.PRECISE_PLACEMENT -> Pair(calculatePrecise(prevPos, precisePlacementSideNum), calculatePrecise(pos, precisePlacementSideNum))
    }
}

fun calculatePrecise(raycastResult: RaycastFunctions.RaycastResult, precisePlacementSideNum: Int = 3): Vector3d {
    val centered = raycastResult.globalCenteredHitPos!!
    val globalNormal = raycastResult.globalNormalDirection!!
    val point = raycastResult.globalHitPos!!

    val up = when {
        globalNormal.x > 0.5 || globalNormal.x < -0.5 -> Vector3d(0, 1, 0)
        globalNormal.y > 0.5 || globalNormal.y < -0.5 -> Vector3d(1, 0, 0)
        globalNormal.z > 0.5 || globalNormal.z < -0.5 -> Vector3d(0, 1, 0)
        else -> throw AssertionError("impossible")
    }

    val right = when {
        globalNormal.x > 0.5 || globalNormal.x < -0.5 -> Vector3d(0, 0, 1)
        globalNormal.y > 0.5 || globalNormal.y < -0.5 -> Vector3d(0, 0, 1)
        globalNormal.z > 0.5 || globalNormal.z < -0.5 -> Vector3d(1, 0, 0)
        else -> throw AssertionError("impossible")
    }

    val points = createSpacedPoints(centered, up, right, 1.0, precisePlacementSideNum).reduce { acc, vector3ds -> acc.addAll(vector3ds); acc }

    return points.minBy { (it - point).sqrDist() }
}

fun BaseMode.serverRaycastAndActivateFn(
    mode: PositionModes,
    precisePlacementAssistSideNum: Int,
    level: Level,
    raycastResult: RaycastFunctions.RaycastResult,
    fnToActivate: (
        level: ServerLevel,
        shipId: ShipId,
        ship: ServerShip?,
        spoint: Vector3d,
        rpoint: Vector3d,
        rresult: RaycastFunctions.RaycastResult) -> Unit
) {
    if (level !is ServerLevel) {throw RuntimeException("Function intended for server use only was activated on client. How.")}
    if (raycastResult.state.isAir) {return}

    val ship = raycastResult.ship as ServerShip?
    val shipId = raycastResult.shipId!!

    val spoint = getModePosition(mode, raycastResult, precisePlacementAssistSideNum)
    val rpoint = if (ship == null) spoint else posShipToWorld(ship, Vector3d(spoint))

    fnToActivate(level, shipId, ship, spoint, rpoint, raycastResult)
}

fun BaseMode.serverRaycast2PointsFnActivation(
    mode: PositionModes,
    precisePlacementAssistSideNum: Int,
    level: Level,
    raycastResult: RaycastFunctions.RaycastResult,
    processNewResult: (RaycastFunctions.RaycastResult) -> Pair<Boolean, RaycastFunctions.RaycastResult?>,
    resetFn: () -> Unit,
    fnToActivate: (
        level: ServerLevel,
        shipId1: ShipId,
        shipId2: ShipId,
        ship1: ServerShip?,
        ship2: ServerShip?,
        spoint1: Vector3d,
        spoint2: Vector3d,
        rpoint1: Vector3d,
        rpoint2: Vector3d,
        prresult: RaycastFunctions.RaycastResult,
        rresult: RaycastFunctions.RaycastResult) -> Unit
) {
    if (level !is ServerLevel) {throw RuntimeException("Function intended for server use only was activated on client. How.")}
    if (raycastResult.state.isAir) {return}
    val (res, previousResult) = processNewResult(raycastResult)
    if (!res) {return}

    val ship1 = previousResult!!.ship as ServerShip?
    val ship2 = raycastResult.ship as ServerShip?

    if (ship1 == null && ship2 == null) { resetFn(); return }
    if (ship1 == ship2) { resetFn(); return }

    val shipId1: ShipId = previousResult.shipId!!
    val shipId2: ShipId = raycastResult.shipId!!

    val (spoint1, spoint2) = getModePositions(mode, previousResult, raycastResult, precisePlacementAssistSideNum)

    val rpoint1 = if (ship1 == null) spoint1 else posShipToWorld(ship1, Vector3d(spoint1))
    val rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

    fnToActivate(level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, previousResult, raycastResult)
}