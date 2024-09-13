package net.spaceeye.vmod.constraintsManaging.types

import gg.essential.elementa.state.map
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.copy
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
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

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = RopeMConstraint()

        new.attachmentPoints_ = copyAttachmentPoints(mainConstraint, attachmentPoints_, level, mapped)
        new.mainConstraint = mainConstraint.copy(level, mapped) ?: return null

        return new
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
        mc(mainConstraint, cIDs, level) {return false}
        return true
    }
}