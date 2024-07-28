package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

class RopeMConstraint(): MConstraint, MRenderable {
    lateinit var constraint: VSRopeConstraint
    override var renderer: BaseRenderer? = null

    var attachmentPoints_ = mutableListOf<BlockPos>()

    var vsId: ConstraintId = -1
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
        constraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        this.renderer = renderer
        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "RopeMConstraint"
    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(constraint.shipId0)
        val ship2Exists = allShips.contains(constraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(constraint.shipId1))
                || (ship2Exists && dimensionIds.contains(constraint.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(constraint.shipId0)) {toReturn.add(constraint.shipId0)}
        if (!dimensionIds.contains(constraint.shipId1)) {toReturn.add(constraint.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        level.shipObjectWorld.removeConstraint(vsId)

        val shipIds = mutableListOf(constraint.shipId0, constraint.shipId1)
        val localPoints = mutableListOf(
            listOf(constraint.localPos0),
            listOf(constraint.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        constraint = constraint.copy(shipIds[0], shipIds[1], constraint.compliance, localPoints[0][0], localPoints[1][0])

        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, constraint, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->
            val con = RopeMConstraint(nShip1?.id ?: constraint.shipId0, nShip2?.id ?: constraint.shipId1, constraint.compliance, localPos0.toJomlVector3d(), localPos1.toJomlVector3d(), constraint.maxForce, constraint.ropeLength, newAttachmentPoints, newRenderer)
            con
        }
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        constraint = constraint.copy(ropeLength = constraint.ropeLength * scaleBy)

        level.shipObjectWorld.removeConstraint(vsId)
        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return setOf(vsId)
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null

        tag.putInt("managedID", mID)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))

        serializeRenderer(tag)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedID")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        deserializeRenderer(tag)

        tryConvertDimensionId(tag, lastDimensionIds); constraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        vsId = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        if (renderer != null) { rID = ServerRenderingData.addRenderer(constraint.shipId0, constraint.shipId1, renderer!!)
        } else { renderer = ServerRenderingData.getRenderer(rID) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(vsId)
        ServerRenderingData.removeRenderer(rID)
    }
}