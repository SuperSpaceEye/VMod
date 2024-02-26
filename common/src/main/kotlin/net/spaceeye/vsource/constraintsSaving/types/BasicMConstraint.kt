package net.spaceeye.vsource.constraintsSaving.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsSaving.ManagedConstraintId
import net.spaceeye.vsource.constraintsSaving.VSConstraintDeserializationUtil
import net.spaceeye.vsource.constraintsSaving.VSConstraintSerializationUtil
import net.spaceeye.vsource.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld

class BasicMConstraint(): MConstraint {
    constructor(constraint_: VSConstraint,
                mID_: ManagedConstraintId) : this() {
        constraint = constraint_
        mID = mID_
    }

    lateinit var constraint: VSConstraint
    var vsID: VSConstraintId = 0

    override val typeName: String get() = "BasicMConstraint"
    override lateinit var mID: ManagedConstraintId
    override val shipId0: ShipId get() = constraint.shipId0
    override val shipId1: ShipId get() = constraint.shipId1

    override fun nbtSerialize(): CompoundTag? {
        val ctag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null
        ctag.putInt("managedID", mID.id)
        return ctag
    }

    //if constraint is between world and ship, then world's id should be in the second shipId of the constraint
    private fun tryConvertDimensionId(ctag: CompoundTag, lastDimensionIds: Map<Long, String>) {
        if (!ctag.contains("shipId1")) {return}

        val id = ctag.getLong("shipId1")
        val dimensionIdStr = lastDimensionIds[id] ?: return
        ctag.putLong("shipId1", ServerLevelHolder.serverLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable[dimensionIdStr]!!)
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag, lastDimensionIds)
        constraint = VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null
        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)

        return this
    }

    override fun addVSConstraintsToLevel(level: ServerLevel): Boolean {
        vsID = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        return true
    }

    override fun removeVSConstraints(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(vsID)
    }
}