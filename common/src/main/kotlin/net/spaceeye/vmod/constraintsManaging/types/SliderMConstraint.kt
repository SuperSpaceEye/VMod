package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.dc
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.constraintsManaging.util.sc
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.copy
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld

class SliderMConstraint(): TwoShipsMConstraint() {
    lateinit var constraint1: VSForceConstraint
    lateinit var constraint2: VSForceConstraint
    lateinit var rconstraint: VSRopeConstraint
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
        val dir = axisSpos2 - axisSpos1
        val length = dir.dist() / 2
        dir.snormalize()

        val sliderCenterPos = sliderSpos1 + (sliderSpos2 - sliderSpos1) / 2
        val axisCenterPos = axisSpos1 + (axisSpos2 - axisSpos1) / 2

        rconstraint = VSRopeConstraint(axisShipId, sliderShipId, compliance,
            axisCenterPos.toJomlVector3d(), sliderCenterPos.toJomlVector3d(), maxForce, length
        )

        constraint1 = VSSlideConstraint(axisShipId, sliderShipId, compliance,
            axisSpos1.toJomlVector3d(), sliderSpos1.toJomlVector3d(),
            maxForce, dir.snormalize().toJomlVector3d(), 1e20,
        )

        constraint2 = VSSlideConstraint(axisShipId, sliderShipId, compliance,
            axisSpos2.toJomlVector3d(), sliderSpos2.toJomlVector3d(),
            maxForce, dir.snormalize().toJomlVector3d(), 1e20,
        )

        attachmentPoints_.addAll(attachmentPoints)
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = SliderMConstraint()

        new.attachmentPoints_ = copyAttachmentPoints(constraint1, attachmentPoints_, level, mapped)

        new.constraint1 = constraint1.copy(level, mapped) ?: return null
        new.constraint2 = constraint2.copy(level, mapped) ?: return null
        new.rconstraint = rconstraint.copy(level, mapped) ?: return null

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        level.shipObjectWorld.removeConstraint(cIDs[2])
        rconstraint = rconstraint.copy(ropeLength = rconstraint.ropeLength * scaleBy)
        cIDs[2] = level.shipObjectWorld.createNewConstraint(rconstraint)!!
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()
        sc("c1", constraint1, tag) {return null}
        sc("c2", constraint2, tag) {return null}
        sc("c3", rconstraint, tag) {return null}
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        dc("c1", ::constraint1, tag, lastDimensionIds) {return null}
        dc("c2", ::constraint2, tag, lastDimensionIds) {return null}
        dc("c3", ::rconstraint, tag, lastDimensionIds) {return null}
        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        mc(constraint1, cIDs, level) {return false}
        mc(constraint2, cIDs, level) {return false}
        mc(rconstraint, cIDs, level) {return false}
        return true
    }
}