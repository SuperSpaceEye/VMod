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
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class AABBWeldCreatorItem : BaseTool() {
    var blockPos: BlockPos? = null

    override fun resetState() {
        LOG("RESETTING STATE")
        blockPos = null
    }

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

        var ship1AttachmentPoints: MutableList<Vector3d> = mutableListOf()
        var ship2AttachmentPoints: MutableList<Vector3d> = mutableListOf()

        val saabbF = ship1?.shipAABB ?: AABBi(blockPos!!.x, blockPos!!.y, blockPos!!.z, blockPos!!.x+1, blockPos!!.y+1, blockPos!!.z+1)
        val saabbS = ship2?.shipAABB ?: AABBi(blockPos!!.x, blockPos!!.y, blockPos!!.z, blockPos!!.x+1, blockPos!!.y+1, blockPos!!.z+1)

        val aabbFA = mutableListOf(
            Vector3d(saabbF.minX(), saabbF.minY(), saabbF.minZ()),
            Vector3d(saabbF.maxX(), saabbF.minY(), saabbF.minZ()),
            Vector3d(saabbF.minX(), saabbF.minY(), saabbF.maxZ()),
            Vector3d(saabbF.maxX(), saabbF.minY(), saabbF.maxZ()),

            Vector3d(saabbF.minX(), saabbF.maxY(), saabbF.minZ()),
            Vector3d(saabbF.maxX(), saabbF.maxY(), saabbF.minZ()),
            Vector3d(saabbF.minX(), saabbF.maxY(), saabbF.maxZ()),
            Vector3d(saabbF.maxX(), saabbF.maxY(), saabbF.maxZ()),
        )

        val aabbSA = mutableListOf(
            Vector3d(saabbS.minX(), saabbS.minY(), saabbS.minZ()),
            Vector3d(saabbS.maxX(), saabbS.minY(), saabbS.minZ()),
            Vector3d(saabbS.minX(), saabbS.minY(), saabbS.maxZ()),
            Vector3d(saabbS.maxX(), saabbS.minY(), saabbS.maxZ()),

            Vector3d(saabbS.minX(), saabbS.maxY(), saabbS.minZ()),
            Vector3d(saabbS.maxX(), saabbS.maxY(), saabbS.minZ()),
            Vector3d(saabbS.minX(), saabbS.maxY(), saabbS.maxZ()),
            Vector3d(saabbS.maxX(), saabbS.maxY(), saabbS.maxZ()),
        )

        // diagonal connections
        ship1AttachmentPoints.add(aabbFA[0])
        ship2AttachmentPoints.add(aabbSA[7])

        ship1AttachmentPoints.add(aabbFA[1])
        ship2AttachmentPoints.add(aabbSA[6])

        ship1AttachmentPoints.add(aabbFA[2])
        ship2AttachmentPoints.add(aabbSA[5])

        ship1AttachmentPoints.add(aabbFA[3])
        ship2AttachmentPoints.add(aabbSA[4])

        // edge connections
        for ((point1, point2) in aabbFA.zip(aabbSA)) {
            ship1AttachmentPoints.add(point1)
            ship2AttachmentPoints.add(point2)
        }

        for ((point1, point2) in ship1AttachmentPoints.zip(ship2AttachmentPoints)) {
            val rpoint1 = if (ship1 == null) point1 else posShipToWorld(ship1, point1)
            val rpoint2 = if (ship2 == null) point2 else posShipToWorld(ship2, point2)

            val constraint = VSAttachmentConstraint(
                shipId1, shipId2,
                1e-10,
                point1.toJomlVector3d(), point2.toJomlVector3d(),
                1e10, (rpoint1 - rpoint2).dist()
            )
            level.makeManagedConstraint(constraint)
        }

        resetState()
    }
}