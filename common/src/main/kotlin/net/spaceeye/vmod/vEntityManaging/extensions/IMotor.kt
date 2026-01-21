package net.spaceeye.vmod.vEntityManaging.extensions

import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.Tickable
import org.joml.Quaterniond
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.world.VsiPhysLevel

interface IMotor: Tickable {
    var sPos1: Vector3d
    var sPos2: Vector3d

    var shipId1: Long
    var shipId2: Long

    var sRot1: Quaterniond
    var sRot2: Quaterniond

    var sDir1: Vector3d
    var sDir2: Vector3d


    var rollAngle: Double
    var pitchAngle: Double
    var yawAngle: Double

    var rollAngularSpeed: Double
    var pitchAngularSpeed: Double
    var yawAngularSpeed: Double

    var doWork: Boolean

    fun wrapAngle(angle: Double): Double = when {
        angle >= 2*Math.PI*2 -> angle - 2*Math.PI*2
        angle < 0.0 -> angle + 2*Math.PI*2
        else -> angle
    }

    fun makeRotatedJoints(f1: VSFixedJoint, f2: VSFixedJoint, distance: Double): Pair<VSFixedJoint, VSFixedJoint> {
        //Uses sDir2 so that rotation is relative to second ship
        var dir = sDir2.toJomlVector3d()
        var axisA = dir.cross(JVector3d(0.0, 1.0, 0.0), JVector3d())
        if (axisA.lengthSquared() < 1e-12) {
            axisA = dir.cross(JVector3d(1.0, 0.0, 0.0), JVector3d())
        }
        axisA.normalize()
        var axisB = dir.cross(axisA, JVector3d()).normalize()

        dir   = sRot1.transformInverse(sRot2.transform(dir))
        axisA = sRot1.transformInverse(sRot2.transform(axisA))
        axisB = sRot1.transformInverse(sRot2.transform(axisB))

        val rotation1 = Quaterniond()
            .rotateAxis(rollAngle, dir)
            .rotateAxis(pitchAngle, axisA)
            .rotateAxis(yawAngle, axisB)

        //first rotates, second maintains distance
        // except for roll, with roll it doesn't matter

        val rot2 = sRot2.get(Quaterniond())
            .invert()

        val rot1 = Quaterniond()
            .mul(sRot1)
            .mul(rotation1)
            .invert()

        val sDir1 = Vector3d(rotation1.invert(Quaterniond()).transform(sDir1.toJomlVector3d()))

        val p11 = sPos1.toJomlVector3d()
        val p21 = (sPos2 - (sDir2 * distance)).toJomlVector3d()
        val p12 = (sPos1 + (sDir1 * distance)).toJomlVector3d()
        val p22 = sPos2.toJomlVector3d()

        val n1 = f1.copy(
            pose0=VSJointPose(p11, rot1),
            pose1=VSJointPose(p21, rot2))
        val n2 = f2.copy(
            pose0=VSJointPose(p12, rot1),
            pose1=VSJointPose(p22, rot2))

        return n1 to n2
    }

    override fun physTick(level: VsiPhysLevel, delta: Double) {
        if (!doWork) return

        val isStatic1 = if (shipId1 == -1L) true else level.getShipById(shipId1)?.isStatic != false
        val isStatic2 = if (shipId2 == -1L) true else level.getShipById(shipId2)?.isStatic != false

        if (isStatic1 && isStatic2) return

        rollAngle  += rollAngularSpeed  / 60.0
        pitchAngle += pitchAngularSpeed / 60.0
        yawAngle   += yawAngularSpeed   / 60.0

        rollAngle  = wrapAngle(rollAngle)
        pitchAngle = wrapAngle(pitchAngle)
        yawAngle   = wrapAngle(yawAngle)
    }
}