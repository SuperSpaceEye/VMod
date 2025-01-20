package net.spaceeye.vmod.utils.vs

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.utils.getMapper
import org.valkyrienskies.core.apigame.joints.VSJoint


//TODO redo everything
object VSJointSerializationUtil {
//    private fun saveBaseConstraint(constraint: VSJoint): CompoundTag {
//        val cTag = CompoundTag()
//        cTag.putLong("shipId0", constraint.shipId0)
//        cTag.putLong("shipId1", constraint.shipId1)
//        cTag.putDouble("compliance", constraint.compliance)
//        cTag.putString("constraintType", constraint.constraintType.toString())
//        return cTag
//    }
//
//    private fun saveForceConstraint(constraint: VSForceConstraint, cTag: CompoundTag): CompoundTag {
//        cTag.putVector3d("localPos0", constraint.localPos0)
//        cTag.putVector3d("localPos1", constraint.localPos1)
//        cTag.putDouble("maxForce", constraint.maxForce)
//        return cTag
//    }
//
//    private fun saveTorqueConstraint(constraint: VSTorqueConstraint, cTag: CompoundTag): CompoundTag {
//        cTag.putQuaterniond("localRot0", constraint.localRot0)
//        cTag.putQuaterniond("localRot1", constraint.localRot1)
//        cTag.putDouble("maxTorque", constraint.maxTorque)
//        return cTag
//    }
//
//    private fun serializeForceConstraint(constraint: VSForceConstraint, cTag: CompoundTag): CompoundTag? {
//        val cTag = saveForceConstraint(constraint, cTag)
//        when (constraint.constraintType) {
//            VSJointType.ATTACHMENT  -> { cTag.putDouble("fixedDistance", (constraint as VSAttachmentConstraint).fixedDistance) }
//            VSJointType.POS_DAMPING -> { cTag.putDouble("posDamping",    (constraint as VSPosDampingConstraint).posDamping) }
//            VSJointType.ROPE        -> { cTag.putDouble("ropeLength",    (constraint as VSRopeConstraint).ropeLength) }
//            VSJointType.SLIDE -> {
//                constraint as VSSlideConstraint
//                cTag.putVector3d("localSlideAxis0", constraint.localSlideAxis0)
//                cTag.putDouble("maxDistBetweenPoints", constraint.maxDistBetweenPoints)
//            }
//            else -> { WLOG("Can't save type ${constraint.constraintType} in VSForceConstraint block"); return null}
//        }
//
//        return cTag
//    }
//
//    private fun serializeTorqueConstraint(constraint: VSTorqueConstraint, cTag: CompoundTag): CompoundTag? {
//        val cTag = saveTorqueConstraint(constraint, cTag)
//
//        when (constraint.constraintType) {
//            VSJointType.HINGE_SWING_LIMITS -> {
//                constraint as VSHingeSwingLimitsConstraint
//                cTag.putDouble("minSwingAngle", constraint.minSwingAngle)
//                cTag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
//            }
//            VSJointType.HINGE_TARGET_ANGLE -> {
//                constraint as VSHingeTargetAngleConstraint
//                cTag.putDouble("targetAngle", constraint.targetAngle)
//                cTag.putDouble("nextTickTargetAngle", constraint.nextTickTargetAngle)
//            }
//            VSJointType.ROT_DAMPING -> {
//                constraint as VSRotDampingConstraint
//                cTag.putDouble("rotDamping", constraint.rotDamping)
//                cTag.putString("rotDampingAxes", constraint.rotDampingAxes.toString())
//            }
//            VSJointType.SPHERICAL_SWING_LIMITS -> {
//                constraint as VSSphericalSwingLimitsConstraint
//                cTag.putDouble("minSwingAngle", constraint.minSwingAngle)
//                cTag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
//            }
//            VSJointType.SPHERICAL_TWIST_LIMITS -> {
//                constraint as VSSphericalTwistLimitsConstraint
//                cTag.putDouble("minTwistAngle", constraint.minTwistAngle)
//                cTag.putDouble("maxTwistAngle", constraint.maxTwistAngle)
//            }
//            VSJointType.FIXED_ORIENTATION -> {}
//            VSJointType.HINGE_ORIENTATION -> {}
//            else -> { WLOG("Can't save type ${constraint.constraintType} in VSForceConstraint block"); return null}
//        }
//        return cTag
//    }

    fun serializeConstraint(constraint: VSJoint): CompoundTag? {
//        var cTag: CompoundTag? = saveBaseConstraint(constraint)

//        when (constraint) {
//            is VSForceConstraint -> cTag = serializeForceConstraint(constraint, cTag!!)
//            is VSTorqueConstraint -> cTag = serializeTorqueConstraint(constraint, cTag!!)
//            else -> {WLOG("Constraint type ${constraint.constraintType} is not VSForceConstraint OR VSTorqueConstraint. Can't serialize"); return null}
//        }

        val mapper = getMapper()
        val tag = CompoundTag()
        tag.putString("type", constraint.jointType.name)
        tag.putLong("shipId0", constraint.shipId0!!)
        tag.putLong("shipId1", constraint.shipId1!!)

        try {
            tag.putByteArray("constraint", mapper.writeValueAsBytes(constraint))
        } catch (e: Exception) {return null
        } catch (e: Error) {return null}

        return tag
    }
}