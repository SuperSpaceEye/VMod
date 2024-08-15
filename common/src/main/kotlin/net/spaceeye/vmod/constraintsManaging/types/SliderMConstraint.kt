package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.commonCopy
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.mod.common.shipObjectWorld

class SliderMConstraint(): TwoShipsMConstraint("SliderMConstraint") {
    lateinit var constraint1: VSAttachmentConstraint
    lateinit var constraint2: VSAttachmentConstraint
    override val mainConstraint: VSConstraint get() = constraint1

    constructor(
        axisShipId: ShipId,
        sliderShipId: ShipId,

        axisSpos1: Vector3d,
        axisSpos2: Vector3d,
        sliderSpos1: Vector3d,
        sliderSpos2: Vector3d,

        compliance: Double,
        maxForce: Double,

        attachmentPoints: List<BlockPos>,
    ): this() {
        constraint1 = VSAttachmentConstraint(
            axisShipId, sliderShipId, compliance,
            axisSpos1.toJomlVector3d(), sliderSpos1.toJomlVector3d(),
            maxForce, 0.0
        )

        constraint2 = VSAttachmentConstraint(
            axisShipId, sliderShipId, compliance,
            axisSpos2.toJomlVector3d(), sliderSpos2.toJomlVector3d(),
            maxForce, 0.0
        )

        attachmentPoints_.addAll(attachmentPoints)
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, constraint1, listOf(attachmentPoints_[0], attachmentPoints_[1])) {
            _, _, _, _, axis1, ship1, newAPPair1 ->
            commonCopy(level, mapped, constraint2, listOf(attachmentPoints_[2], attachmentPoints_[3])) {
                nShip1Id, nShip2Id, nShip1, nShip2, axis2, ship2, newAPPair2 ->
                SliderMConstraint(nShip1Id, nShip2Id, axis1, axis2, ship1, ship2,
                    constraint1.compliance, constraint1.maxForce,
                    listOf(newAPPair1[0], newAPPair1[1], newAPPair2[0], newAPPair2[1]))
            }
        }
    }

    // it doesn't need to do anything
    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()
        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(constraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(constraint2) ?: return null)
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); constraint1 = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); constraint2 = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint1) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint2) ?: clean(level) ?: return false)

        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}