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
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint

class SyncRotationMConstraint(): TwoShipsMConstraint() {
    override lateinit var mainConstraint: VSJoint

    constructor(
        shipId1: ShipId,
        shipId2: ShipId,

        srot1: Quaterniondc,
        srot2: Quaterniondc,

        compliance: Double,
        maxForce: Double
    ): this() {
//        mainConstraint = VSFixedOrientationConstraint(shipId1, shipId2, compliance,
//            srot1.invert(Quaterniond()),
//            srot2.invert(Quaterniond()),
//            maxForce
//            )
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = SyncRotationMConstraint()
//        new.mainConstraint = mainConstraint.copy(mapped) ?: return null
        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mID", mID)

        sc("c1", mainConstraint, tag) {return null}

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("mID")

        dc("c1", ::mainConstraint, tag, lastDimensionIds) {return null}

        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        mc(mainConstraint, cIDs, level) {return false}
        return true
    }
}