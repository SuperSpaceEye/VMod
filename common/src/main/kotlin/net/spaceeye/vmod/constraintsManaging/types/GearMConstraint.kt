package net.spaceeye.vmod.constraintsManaging.types

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.MCAutoSerializable
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointPose
import net.spaceeye.vmod.networking.TagSerializableItem.get
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.valkyrienskies.core.apigame.joints.VSGearJoint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque

class GearMConstraint(): TwoShipsMConstraint(), MCAutoSerializable {
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

    var gearRatio: Float = 1f

     constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        sDir1: Vector3d,
        sDir2: Vector3d,

        sRot1: Quaterniond,
        sRot2: Quaterniond,


        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        gearRatio: Float,

        attachmentPoints: List<BlockPos>,
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.maxForce = maxForce
        this.gearRatio = gearRatio

        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
         return GearMConstraint(
             tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
             tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
             sDir1, sDir2, sRot1, sRot2,
             mapped[shipId1] ?: return null,
             mapped[shipId2] ?: return null,
             maxForce, gearRatio,
             copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
//        onDeleteMConstraint(level)
//        onMakeMConstraint(level)
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
        val rotationConstraint = VSGearJoint(
            shipId1, VSJointPose(sPos1.toJomlVector3d(), getHingeRotation(sDir1)),
            shipId2, VSJointPose(sPos2.toJomlVector3d(), getHingeRotation(sDir2)),
            maxForceTorque,
            gearRatio = gearRatio,
            )

        mc(rotationConstraint, cIDs, level) {return false}

        return true
    }
}