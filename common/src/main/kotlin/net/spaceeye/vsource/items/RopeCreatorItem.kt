package net.spaceeye.vsource.items

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.utils.posShipToWorld
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

// TODO make rope ends actual ray hit positions and not just at the center of the blocks
class RopeCreatorItem: BaseTool() {
    var blockPos: BlockPos? = null

    override fun activatePrimaryFunction(level: Level, player: Player, clipResult: BlockHitResult) {
        if (level.isClientSide) {return}
        if (level !is ServerLevel) {return}

        if (blockPos == null) {blockPos = clipResult.blockPos; return}
        if (blockPos == clipResult.blockPos) {resetState(); return}

        val ship1 = level.getShipManagingPos(blockPos!!)
        val ship2 = level.getShipManagingPos(clipResult.blockPos)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val point1 = Vector3d(blockPos!!) + 0.5
        val point2 = Vector3d(clipResult.blockPos) + 0.5

        val rpoint1 = if (ship1 == null) point1 else posShipToWorld(ship1, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorld(ship2, point2)

        val constraint = VSRopeConstraint(
            shipId1, shipId2,
            1e-10,
            point1.toJomlVector3d(), point2.toJomlVector3d(),
            1e10,
            (rpoint1 - rpoint2).dist()
        )

        level.makeManagedConstraint(constraint)

        resetState()
    }

    override fun resetState() {
        LOG("RESETTING STATE")
        blockPos = null
    }
}