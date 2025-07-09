package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.valkyrienskies.core.apigame.joints.VSGearJoint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose

class GearConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override var sPos1: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var sPos2: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

    var maxForce: Float by get(i++, -1f)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

    var gearRatio: Float by get(i++, 1f)

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
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
         return GearConstraint(
             tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
             tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
             sDir1, sDir2, sRot1, sRot2,
             mapped[shipId1] ?: return null,
             mapped[shipId2] ?: return null,
             maxForce, gearRatio
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> { return emptyList() }

    override fun iOnMakeVEntity(level: ServerLevel) = withFutures {
        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
        val rotationConstraint = VSGearJoint(
            shipId1, VSJointPose(sPos1.toJomlVector3d(), getHingeRotation(sDir1)),
            shipId2, VSJointPose(sPos2.toJomlVector3d(), getHingeRotation(sDir2)),
            maxForceTorque,
            gearRatio = gearRatio,
        )

        mc(rotationConstraint, level)
    }
}