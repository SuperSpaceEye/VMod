package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSD6Joint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose
import java.util.EnumMap

class SliderConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    //TODO unify and rename values (needs backwards compat)
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    override var sPos1: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var sPos2: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

    var connectionMode: ConnectionMode by get(i++, ConnectionMode.FIXED_ORIENTATION)
    var maxForce: Float by get(i++, -1f)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

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

        connectionMode: ConnectionMode,
    ): this() {
        this.connectionMode = connectionMode
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.maxForce = maxForce
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        return SliderConstraint(
            tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
            tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, connectionMode
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun iOnMakeVEntity(level: ServerLevel) = withFutures {
        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
//        val stiffness = if (stiffness < 0) {null} else {stiffness}
//        val damping = if (damping < 0) {null} else {damping}

        val distanceConstraint = run {
            val rot1 = getHingeRotation(-sDir1.normalize())
            val rot2 = getHingeRotation(-sDir2.normalize())
            VSD6Joint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                motions = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),

                    Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                )),
//                linearLimits = EnumMap(mapOf(
//                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(this.distance, this.distance)))
//                ),
                maxForceTorque = maxForceTorque
            )
        }
        mc(distanceConstraint, level)
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return@withFutures}

        val rotationConstraint = when(connectionMode) {
            ConnectionMode.FIXED_ORIENTATION -> {
                val rot1 = sRot1.invert(Quaterniond())
                val rot2 = sRot2.invert(Quaterniond())
                VSD6Joint(
                    shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                    shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                    )),
                    maxForceTorque = maxForceTorque
                )
            }
            ConnectionMode.HINGE_ORIENTATION -> {
                val rot1 = getHingeRotation(sDir1)
                val rot2 = getHingeRotation(sDir2)
                VSD6Joint(
                    shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                    shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE)
                    )),
                    maxForceTorque = maxForceTorque
                )
            }
            ConnectionMode.FREE_ORIENTATION -> throw AssertionError("impossible")
        }
        mc(rotationConstraint, level)
    }
}