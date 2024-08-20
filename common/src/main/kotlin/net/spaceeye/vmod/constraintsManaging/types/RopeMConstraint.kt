package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld

class RopeMConstraint(): TwoShipsMConstraint() {
    override lateinit var mainConstraint: VSRopeConstraint

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,
        ropeLength: Double,
        attachmentPoints: List<BlockPos>,
    ): this() {
        mainConstraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        level.shipObjectWorld.removeConstraint(cIDs[0])

        val shipIds = mutableListOf(mainConstraint.shipId0, mainConstraint.shipId1)
        val localPoints = mutableListOf(
            listOf(mainConstraint.localPos0),
            listOf(mainConstraint.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        mainConstraint = mainConstraint.copy(shipIds[0], shipIds[1], mainConstraint.compliance, localPoints[0][0], localPoints[1][0])

        cIDs[0] = level.shipObjectWorld.createNewConstraint(mainConstraint)!!
//        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, mainConstraint, attachmentPoints_) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints ->
            val con = RopeMConstraint(nShip1?.id ?: mainConstraint.shipId0, nShip2?.id ?: mainConstraint.shipId1, mainConstraint.compliance, localPos0.toJomlVector3d(), localPos1.toJomlVector3d(), mainConstraint.maxForce, mainConstraint.ropeLength, newAttachmentPoints)
            con
        }
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        mainConstraint = mainConstraint.copy(ropeLength = mainConstraint.ropeLength * scaleBy)

        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(mainConstraint)!!
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(mainConstraint) ?: return null
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag, lastDimensionIds); mainConstraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint
        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(mainConstraint) ?: return false)
        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(cIDs[0])
    }
}