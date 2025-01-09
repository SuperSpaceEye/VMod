package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.dc
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.constraintsManaging.util.sc
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.*
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.EnumMap

class ConnectionMConstraint(): TwoShipsMConstraint() {
    enum class ConnectionModes {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    lateinit var distanceConstraint: VSJoint
    lateinit var rotationConstraint: VSD6Joint

    override val mainConstraint: VSJoint get() = distanceConstraint

    var fixedLength: Double = 0.0
    var connectionMode: ConnectionModes = ConnectionModes.FIXED_ORIENTATION

    //TODO add more parameters?
    constructor(
        // shipyard pos
        spoint1: Vector3d,
        spoint2: Vector3d,
        // world pos
        rpoint1: Vector3d,
        rpoint2: Vector3d,
        ship1: Ship?,
        ship2: Ship?,
        shipId0: ShipId,
        shipId1: ShipId,
        maxForce: Float,

        fixedLength: Double,
        connectionMode: ConnectionModes,

        attachmentPoints: List<BlockPos>,

        _dir: Vector3d? = null
    ): this() {
        this.fixedLength = if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength
        this.connectionMode = connectionMode
        attachmentPoints_ = attachmentPoints.toMutableList()

        val dist1 = rpoint1 - rpoint2
        val dir = (_dir ?: run { dist1.normalize() })

        distanceConstraint = if (connectionMode == ConnectionModes.FREE_ORIENTATION) {
            VSDistanceJoint(
                shipId0, VSJointPose(spoint1.toJomlVector3d(), Quaterniond()),
                shipId1, VSJointPose(spoint2.toJomlVector3d(), Quaterniond()),
                VSJointMaxForceTorque(maxForce, maxForce),
                this.fixedLength.toFloat(),
                this.fixedLength.toFloat(),
            )
        } else {
            val rot1 = getHingeRotation(ship1?.transform, -dir.normalize())
            val rot2 = getHingeRotation(ship2?.transform, -dir.normalize())
            VSD6Joint(
                shipId0, VSJointPose(spoint1.toJomlVector3d(), rot1),
                shipId1, VSJointPose(spoint2.toJomlVector3d(), rot2),
                motions = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.LIMITED),

                    Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                )),
                linearLimits = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(this.fixedLength.toFloat(), this.fixedLength.toFloat())))
                ),
                maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
            )
        }

        rotationConstraint = when(connectionMode) {
            ConnectionModes.FIXED_ORIENTATION -> {
                val rot1 = (ship1?.transform?.shipToWorldRotation ?: Quaterniond()).invert(Quaterniond())
                val rot2 = (ship2?.transform?.shipToWorldRotation ?: Quaterniond()).invert(Quaterniond())
                VSD6Joint(
                    shipId0, VSJointPose(spoint1.toJomlVector3d(), rot1),
                    shipId1, VSJointPose(spoint2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                    )),
                    maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
                )
            }
            ConnectionModes.HINGE_ORIENTATION -> {
                val rot1 = getHingeRotation(ship1?.transform, dir.normalize())
                val rot2 = getHingeRotation(ship2?.transform, dir.normalize())
                VSD6Joint(
                    shipId0, VSJointPose(spoint1.toJomlVector3d(), rot1),
                    shipId1, VSJointPose(spoint2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE)
                    )),
                    maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
                )
            }
            ConnectionModes.FREE_ORIENTATION -> {
                VSD6Joint(
                    shipId0, VSJointPose(spoint1.toJomlVector3d(), Quaterniond()),
                    shipId1, VSJointPose(spoint2.toJomlVector3d(), Quaterniond()),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                    )),
                    maxForceTorque = VSJointMaxForceTorque(maxForce, maxForce)
                )
            }
        }
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = ConnectionMConstraint()

        new.fixedLength = fixedLength
        new.connectionMode = connectionMode
        new.attachmentPoints_ = copyAttachmentPoints(distanceConstraint, attachmentPoints_, level, mapped)

        new.distanceConstraint = distanceConstraint.copy(level, mapped) ?: return null
        new.rotationConstraint = rotationConstraint.copy(level, mapped) ?: return null

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        distanceConstraint = when (distanceConstraint) {
            is VSDistanceJoint -> {
                val c = distanceConstraint as VSDistanceJoint
                c.copy(minDistance = c.minDistance!! * scaleBy.toFloat(), maxDistance = c.maxDistance!! * scaleBy.toFloat())
            }
            is VSD6Joint -> {
                val c = distanceConstraint as VSD6Joint
                val m = c.linearLimits!!.clone()
                val l = m[VSD6Joint.D6Axis.X]!!
                m[VSD6Joint.D6Axis.X] = l.copy(lowerLimit = l.lowerLimit * scaleBy.toFloat(), upperLimit = l.upperLimit * scaleBy.toFloat())
                c.copy(linearLimits = m)
            }
            else -> throw AssertionError("Should be impossible.")
        }

        level.shipObjectWorld.updateConstraint(cIDs[0], distanceConstraint)
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mode", connectionMode.ordinal)

        sc("c1", distanceConstraint, tag) {return null}
        sc("c2", rotationConstraint, tag) {return null}

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        connectionMode = ConnectionModes.values()[tag.getInt("mode")]

        dc("c1", ::distanceConstraint, tag, lastDimensionIds) {return null}
        dc("c2", ::rotationConstraint, tag, lastDimensionIds) {return null}

        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        mc(distanceConstraint, cIDs, level) {return false}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return true }
        mc(rotationConstraint, cIDs, level) {return false}

        return true
    }
}