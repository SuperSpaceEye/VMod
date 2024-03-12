package net.spaceeye.vsource.toolgun.modes

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.posShipToWorld
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.reflect.KMutableProperty0

enum class PositionModes {
    NORMAL,
    CENTERED_ON_SIDE,
    CENTERED_IN_BLOCK,
}

fun getModePositions(mode: PositionModes, prevPos: RaycastFunctions.RaycastResult, pos: RaycastFunctions.RaycastResult): Pair<Vector3d, Vector3d> {
    return when(mode) {
        PositionModes.NORMAL -> Pair(prevPos.globalHitPos!!, pos.globalHitPos!!)
        PositionModes.CENTERED_ON_SIDE -> Pair(prevPos.globalCenteredHitPos!!, pos.globalCenteredHitPos!!)
        PositionModes.CENTERED_IN_BLOCK -> Pair(Vector3d(prevPos.blockPosition!!) + 0.5, Vector3d(pos.blockPosition!!) + 0.5)
    }
}

fun BaseMode.serverTryActivateFunction(
    mode: PositionModes,
    level: Level,
    raycastResult: RaycastFunctions.RaycastResult,
    previousResult: KMutableProperty0<RaycastFunctions.RaycastResult?>,
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
    if (previousResult.get() == null) {previousResult.set(raycastResult); return}

    val ship1 = level.getShipManagingPos(previousResult.get()!!.blockPosition!!)
    val ship2 = level.getShipManagingPos(raycastResult.blockPosition!!)

    if (ship1 == null && ship2 == null) { resetFn(); return }
    if (ship1 == ship2) { resetFn(); return }

    val shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
    val shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

    val (spoint1, spoint2) = getModePositions(mode, previousResult.get()!!, raycastResult)

    val rpoint1 = if (ship1 == null) spoint1 else posShipToWorld(ship1, Vector3d(spoint1))
    val rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

    fnToActivate(level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, previousResult.get()!!, raycastResult)
}