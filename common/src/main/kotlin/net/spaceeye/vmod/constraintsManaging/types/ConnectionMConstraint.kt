package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.NewTwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.networking.TagAutoSerializable
import net.spaceeye.vmod.networking.TagSerializableItem.get
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.*
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.*
import java.util.EnumMap

class ConnectionMConstraint(): NewTwoShipsMConstraint(), TagAutoSerializable {
    enum class ConnectionModes {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    var connectionMode: ConnectionModes by get(0, ConnectionModes.FIXED_ORIENTATION)
    var distance: Double by get(1, 0.0)
    var maxForce: Float by get(2, -1f)
    var wDir: Vector3d by get(3, Vector3d())

    var sRot1: Quaterniond by get(4, Quaterniond())
    var sRot2: Quaterniond by get(5, Quaterniond())

    var sDir1: Vector3d by get(6, Vector3d())
    var sDir2: Vector3d by get(7, Vector3d())

    //TODO add more parameters?
    constructor(
        // shipyard pos
        sPos1: Vector3d,
        sPos2: Vector3d,
        // world pos
        rPos1: Vector3d,
        rPos2: Vector3d,
        ship1: Ship?,
        ship2: Ship?,
        shipId1: ShipId,
        shipId2: ShipId,
        maxForce: Float,

        fixedLength: Double,
        connectionMode: ConnectionModes,

        attachmentPoints: List<BlockPos>,

        _wDir: Vector3d? = null
    ): this() {
        this.distance = if (fixedLength < 0) (rPos1 - rPos2).dist() else fixedLength
        this.connectionMode = connectionMode
        attachmentPoints_ = attachmentPoints.toMutableList()
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.maxForce = maxForce

        wDir = ((_wDir ?: (rPos1 - rPos2))).normalize()

        sDir1 = ship1?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy()
        sDir2 = ship2?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy()

        sRot1 = Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond())
        sRot2 = Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond())
     }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = ConnectionMConstraint(
            tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
            tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
            Vector3d(), Vector3d(), null, null,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, distance, connectionMode,
            copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
            wDir
        )
        new.sDir1 = sDir1.copy()
        new.sDir2 = sDir2.copy()
        new.sRot1 = Quaterniond(sRot1)
        new.sRot2 = Quaterniond(sRot2)

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        distance *= scaleBy
        onDeleteMConstraint(level)
        onMakeMConstraint(level)
    }

    override fun iNbtSerialize(): CompoundTag? = tSerialize()
    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>) = tDeserialize(tag).let { this }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        val distanceConstraint = if (connectionMode == ConnectionModes.FREE_ORIENTATION) {
            VSDistanceJoint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
                VSJointMaxForceTorque(maxForce, maxForce),
                this.distance.toFloat(),
                this.distance.toFloat(),
            )
        } else {
            val rot1 = getHingeRotation(-sDir1.normalize())
            val rot2 = getHingeRotation(-sDir2.normalize())
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
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(this.distance.toFloat(), this.distance.toFloat())))
                ),
                maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
            )
        }
        mc(distanceConstraint, cIDs, level) {return false}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return true }

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
                    maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
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
                    maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
                )
            }
            ConnectionModes.FREE_ORIENTATION -> throw AssertionError("how")
        }

        mc(rotationConstraint, cIDs, level) {return false}

        return true
    }
}