package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.*
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSD6Joint
import org.valkyrienskies.core.apigame.joints.VSDistanceJoint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose
import java.util.EnumMap

class ConnectionConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    //TODO unify and rename values (needs backwards compat)
    enum class ConnectionModes {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }
    override var sPos1: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var sPos2: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

    var connectionMode: ConnectionModes by get(i++, ConnectionModes.FIXED_ORIENTATION)
    var distance: Float by get(i++, 0f)
    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, -1f)
    var damping: Float by get(i++, -1f)

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
        stiffness: Float,
        damping: Float,

        distance: Float,
        connectionMode: ConnectionModes,
        ): this() {
        this.distance = distance
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
        this.stiffness = stiffness
        this.damping = damping
     }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        return ConnectionConstraint(
            tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
            tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
            sDir1.copy(), sDir2.copy(), sRot1.get(Quaterniond()), sRot2.get(Quaterniond()),
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, stiffness, damping, distance, connectionMode
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        distance *= scaleBy.toFloat()
        onDeleteVEntity(level)
        onMakeVEntity(level)
    }

    override fun iOnMakeVEntity(level: ServerLevel) = withFutures {
        if (shipId1 == -1L && shipId2 == -1L) {throw AssertionError("Both shipId's are ground")}
        val (shipId1, shipId2, sPos1, sPos2, sDir1, sDir2, sRot1, sRot2) = when (-1L) {
            shipId1 -> Tuple.of(null   , shipId2, sPos1 + 0.5, sPos2,  sDir1, sDir2, sRot1, sRot2)
            shipId2 -> Tuple.of(null   , shipId1, sPos2 + 0.5, sPos1, -sDir2, sDir1, sRot2, sRot1)
            else    -> Tuple.of(shipId1, shipId2, sPos1      , sPos2,  sDir1, sDir2, sRot1, sRot2)
        }

        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
        val stiffness = if (stiffness < 0) {null} else {stiffness}
        val damping = if (damping < 0) {null} else {damping}

        val distanceConstraint = if (connectionMode == ConnectionModes.FREE_ORIENTATION) {
            VSDistanceJoint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
                maxForceTorque, distance, distance, stiffness = stiffness, damping = damping
            )
        } else {
            val rot1 = getHingeRotation(sDir1.normalize())
            val rot2 = getHingeRotation(sDir2.normalize())
            VSD6Joint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                motions = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.LIMITED),

                    Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                )),
                linearLimits = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(distance, distance, stiffness = stiffness, damping = damping)))
                ),
                maxForceTorque = maxForceTorque
            )
        }
        mc(distanceConstraint, level)
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return@withFutures }

        //TODO do i want limits to rotation constraint?
        val rotationConstraint = when(connectionMode) {
            ConnectionModes.FIXED_ORIENTATION -> {
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
            ConnectionModes.HINGE_ORIENTATION -> {
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
            ConnectionModes.FREE_ORIENTATION -> throw AssertionError("Impossible Situation")
        }

        mc(rotationConstraint, level)
    }
}