package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld

class RopeMConstraint(): TwoShipsMConstraint("RopeMConstraint"), MRenderable {
    override lateinit var mainConstraint: VSRopeConstraint
    override var renderer: BaseRenderer? = null

    var rID: Int = -1

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,
        ropeLength: Double,
        attachmentPoints: List<BlockPos>,
        renderer: BaseRenderer?
    ): this() {
        mainConstraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        this.renderer = renderer
        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
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

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, mainConstraint, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->
            val con = RopeMConstraint(nShip1?.id ?: mainConstraint.shipId0, nShip2?.id ?: mainConstraint.shipId1, mainConstraint.compliance, localPos0.toJomlVector3d(), localPos1.toJomlVector3d(), mainConstraint.maxForce, mainConstraint.ropeLength, newAttachmentPoints, newRenderer)
            con
        }
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        mainConstraint = mainConstraint.copy(ropeLength = mainConstraint.ropeLength * scaleBy)

        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(mainConstraint)!!
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(mainConstraint) ?: return null

        tag.putInt("managedID", mID)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))

        serializeRenderer(tag)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedID")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        deserializeRenderer(tag)

        tryConvertDimensionId(tag, lastDimensionIds); mainConstraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(mainConstraint) ?: return false)
        if (renderer != null) { rID = ServerRenderingData.addRenderer(mainConstraint.shipId0, mainConstraint.shipId1, renderer!!)
        } else { renderer = ServerRenderingData.getRenderer(rID) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(cIDs[0])
        ServerRenderingData.removeRenderer(rID)
    }
}