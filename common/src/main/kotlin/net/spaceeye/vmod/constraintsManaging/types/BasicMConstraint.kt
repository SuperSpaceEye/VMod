package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.VSConstraintDeserializationUtil
import net.spaceeye.vmod.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld

class BasicMConstraint(): MConstraint {
    constructor(constraint_: VSConstraint, attachmentPoints: List<BlockPos> = listOf()) :this() {
        constraint = constraint_
        this.attachmentPoints_ = attachmentPoints
    }
    constructor(constraint_: VSConstraint,
                mID_: ManagedConstraintId) : this() {
        constraint = constraint_
        mID = mID_
    }

    var attachmentPoints_ = listOf<BlockPos>()
    lateinit var constraint: VSConstraint
    var vsID: VSConstraintId = 0

    override val typeName: String get() = "BasicMConstraint"
    override lateinit var mID: ManagedConstraintId
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
        ELOG("MOVING SHIPYARD POSITIONS IS NOT IMPLEMENTED FOR BasicMConstraint")
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null
        tag.putInt("managedID", mID.id)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = ManagedConstraintId(tag.getInt("managedID"))
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)

        tryConvertDimensionId(tag, lastDimensionIds)
        constraint = VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        vsID = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(vsID)
    }
}