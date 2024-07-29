package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSTorqueConstraint
import org.valkyrienskies.mod.common.shipObjectWorld

class SyncRotationMConstraint(): TwoShipsMConstraint("SyncRotationMConstraint") {
    override lateinit var mainConstraint: VSTorqueConstraint

    constructor(
        shipId1: ShipId,
        shipId2: ShipId,

        srot1: Quaterniondc,
        srot2: Quaterniondc,

        compliance: Double,
        maxForce: Double
    ): this() {
        mainConstraint = VSFixedOrientationConstraint(shipId1, shipId2, compliance,
            srot1.invert(Quaterniond()),
            srot2.invert(Quaterniond()),
            maxForce
            )
    }

    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO("Not yet implemented")
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        if (!mapped.keys.contains(mainConstraint.shipId0) && !mapped.keys.contains(mainConstraint.shipId1)) return null

        return SyncRotationMConstraint(
            mapped[mainConstraint.shipId0]!!,
            mapped[mainConstraint.shipId1]!!,
            mainConstraint.localRot0.invert(Quaterniond()),
            mainConstraint.localRot1.invert(Quaterniond()),
            mainConstraint.compliance,
            mainConstraint.maxTorque
        )
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mID", mID)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(mainConstraint) ?: return null)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("mID")

        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); mainConstraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSTorqueConstraint

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(mainConstraint) ?: clean(level) ?: return false)
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}