package net.spaceeye.vmod.constraintsManaging.types

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.util.*
import net.spaceeye.vmod.networking.TagSerializableItem.get
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.*
import java.util.*

class SliderMConstraint(): TwoShipsMConstraint(), MCAutoSerializable {
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    @JsonIgnore private var i = 0

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

        attachmentPoints: List<BlockPos>,
    ): this() {
        this.connectionMode = connectionMode
        attachmentPoints_ = attachmentPoints.toMutableList()
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.maxForce = maxForce

        attachmentPoints_.addAll(attachmentPoints)
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return SliderMConstraint(
            tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
            tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, connectionMode,
            copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
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
        mc(distanceConstraint, cIDs, level) {return false}
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return true}

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
        mc(rotationConstraint, cIDs, level) {return false}

        return true
    }
}