package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.vEntityManaging.util.mc
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.valkyrienskies.core.apigame.joints.VSDistanceJoint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose

class RopeConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override var sPos1: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var sPos2: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, 0f)
    var damping: Float by get(i++, 0f)
    var ropeLength: Float by get(i++, 0f)

     constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        stiffness: Float,
        damping: Float,

        ropeLength: Float
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.maxForce = maxForce
        this.stiffness = stiffness
        this.damping = damping

        this.ropeLength = ropeLength
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
         return RopeConstraint(
             tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
             tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
             mapped[shipId1] ?: return null,
             mapped[shipId2] ?: return null,
             maxForce, stiffness, damping, ropeLength
         )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        ropeLength *= scaleBy.toFloat()
        onDeleteVEntity(level)
        onMakeVEntity(level)
    }

    override fun iOnMakeVEntity(level: ServerLevel) = withFutures {
        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
        val stiffness = if (stiffness < 0) {null} else {stiffness}
        val damping = if (damping < 0) {null} else {damping}

        val mainConstraint = VSDistanceJoint(
            shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
            shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
            maxForceTorque,
            0f, ropeLength,
            stiffness = stiffness,
            damping = damping
        )
        mc(mainConstraint, level)
    }
}