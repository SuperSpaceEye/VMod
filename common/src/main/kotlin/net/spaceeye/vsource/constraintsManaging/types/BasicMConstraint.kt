package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vsource.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vsource.utils.deserializeBlockPositions
import net.spaceeye.vsource.utils.serializeBlockPositions
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

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null
        tag.putInt("managedID", mID.id)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag, lastDimensionIds)
        constraint = VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null
        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)

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