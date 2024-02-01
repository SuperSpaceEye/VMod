package net.spaceeye.vssource.utils

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vssource.LOG
import org.valkyrienskies.core.apigame.constraints.*

object VSConstraintSerializationUtil {
    private fun saveBaseConstraint(constraint: VSConstraint): CompoundTag {
        val cTag = CompoundTag()
        cTag.putLong("shipId0", constraint.shipId0)
        cTag.putLong("shipId1", constraint.shipId1)
        cTag.putDouble("compliance", constraint.compliance)
        cTag.putString("constraintType", constraint.constraintType.toString())
        return cTag
    }

    private fun saveForceConstraint(constraint: VSForceConstraint, cTag: CompoundTag): CompoundTag {
        cTag.putVector3d("localPos0", constraint.localPos0)
        cTag.putVector3d("localPos1", constraint.localPos1)
        cTag.putDouble("maxForce", constraint.maxForce)
        return cTag
    }

    private fun saveTorqueConstraint(constraint: VSTorqueConstraint, cTag: CompoundTag): CompoundTag {
        cTag.putQuaterniond("localRot0", constraint.localRot0)
        cTag.putQuaterniond("localRot1", constraint.localRot1)
        cTag.putDouble("maxTorque", constraint.maxTorque)
        return cTag
    }

    private fun serializeForceConstraint(constraint: VSForceConstraint, cTag: CompoundTag): CompoundTag? {
        val cTag = saveForceConstraint(constraint, cTag)
        when (constraint.constraintType) {
            VSConstraintType.ATTACHMENT  -> { cTag.putDouble("fixedDistance", (constraint as VSAttachmentConstraint).fixedDistance) }
            VSConstraintType.POS_DAMPING -> { cTag.putDouble("posDamping",    (constraint as VSPosDampingConstraint).posDamping) }
            VSConstraintType.ROPE        -> { cTag.putDouble("ropeLength",    (constraint as VSRopeConstraint).ropeLength) }
            VSConstraintType.SLIDE -> {
                constraint as VSSlideConstraint
                cTag.putVector3d("localSlideAxis0", constraint.localSlideAxis0)
                cTag.putDouble("maxDistBetweenPoints", constraint.maxDistBetweenPoints)
            }
            else -> { LOG("CAN'T SAVE TYPE ${constraint.constraintType} IN VSForceConstraint BLOCK"); return null}
        }

        return cTag
    }

    private fun serializeTorqueConstraint(constraint: VSTorqueConstraint, cTag: CompoundTag): CompoundTag? {
        val cTag = saveTorqueConstraint(constraint, cTag)

        when (constraint.constraintType) {
            VSConstraintType.HINGE_SWING_LIMITS -> {
                constraint as VSHingeSwingLimitsConstraint
                cTag.putDouble("minSwingAngle", constraint.minSwingAngle)
                cTag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
            }
            VSConstraintType.HINGE_TARGET_ANGLE -> {
                constraint as VSHingeTargetAngleConstraint
                cTag.putDouble("targetAngle", constraint.targetAngle)
                cTag.putDouble("nextTickTargetAngle", constraint.nextTickTargetAngle)
            }
            VSConstraintType.ROT_DAMPING -> {
                constraint as VSRotDampingConstraint
                cTag.putDouble("rotDamping", constraint.rotDamping)
                cTag.putString("rotDampingAxes", constraint.rotDampingAxes.toString())
            }
            VSConstraintType.SPHERICAL_SWING_LIMITS -> {
                constraint as VSSphericalSwingLimitsConstraint
                cTag.putDouble("minSwingAngle", constraint.minSwingAngle)
                cTag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
            }
            VSConstraintType.SPHERICAL_TWIST_LIMITS -> {
                constraint as VSSphericalTwistLimitsConstraint
                cTag.putDouble("minTwistAngle", constraint.minTwistAngle)
                cTag.putDouble("maxTwistAngle", constraint.maxTwistAngle)
            }
            VSConstraintType.FIXED_ORIENTATION -> {}
            VSConstraintType.HINGE_ORIENTATION -> {}
            else -> { LOG("CAN'T SAVE TYPE ${constraint.constraintType} IN VSForceConstraint BLOCK"); return null}
        }
        return cTag
    }

    fun serializeConstraint(constraint: VSConstraint): CompoundTag? {
        var cTag: CompoundTag? = saveBaseConstraint(constraint)

        when (constraint) {
            is VSForceConstraint -> cTag = serializeForceConstraint(constraint, cTag!!)
            is VSTorqueConstraint -> cTag = serializeTorqueConstraint(constraint, cTag!!)
            else -> {LOG("CONSTRAINT TYPE ${constraint.constraintType} IS NOT VSForceConstraint OR VSTorqueConstraint. CAN'T SERIALIZE."); return null}
        }

        return cTag
    }
}