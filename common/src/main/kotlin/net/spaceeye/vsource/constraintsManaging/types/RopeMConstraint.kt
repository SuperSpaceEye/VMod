package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vsource.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.BaseRenderer
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

class RopeMConstraint(): MConstraint {
    lateinit var constraint: VSRopeConstraint
    private var renderer: BaseRenderer? = null // will not get saved

    var vsId: ConstraintId = -1

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,
        ropeLength: Double,
        renderer: BaseRenderer?
    ): this() {
        constraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        this.renderer = renderer
    }

    override lateinit var mID: ManagedConstraintId
    override val shipId0: ShipId get() = constraint.shipId0
    override val shipId1: ShipId get() = constraint.shipId1
    override val typeName: String get() = "RopeMConstraint"

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null
        tag.putInt("managedID", mID.id)
        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag, lastDimensionIds)
        constraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint
        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        vsId = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        if (renderer != null) {SynchronisedRenderingData.serverSynchronisedData.addRenderer(constraint.shipId0, constraint.shipId1, mID.id, renderer!!)}
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(vsId)
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID.id)
    }
}