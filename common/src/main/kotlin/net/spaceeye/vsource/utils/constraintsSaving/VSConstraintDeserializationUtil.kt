package net.spaceeye.vsource.utils.constraintsSaving

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.utils.getQuaterniond
import net.spaceeye.vsource.utils.getVector3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.core.apigame.constraints.VSConstraintType.*

private open class BaseConstraintData(
    var shipId0: ShipId,
    var shipId1: ShipId,
    var compliance: Double,
    var constraintType: VSConstraintType,
)

private open class ForceConstraintData(
    shipId0: ShipId=0,
    shipId1: ShipId=0,
    compliance: Double=0.0,
    constraintType: VSConstraintType=ATTACHMENT,
    var localPos0: Vector3dc=Vector3d(),
    var localPos1: Vector3dc=Vector3d(),
    var maxForce: Double=0.0
): BaseConstraintData(shipId0, shipId1, compliance, constraintType)

private open class TorqueConstraintData(
    shipId0: ShipId=0,
    shipId1: ShipId=0,
    compliance: Double=0.0,
    constraintType: VSConstraintType=ATTACHMENT,
    var localRot0: Quaterniond=Quaterniond(),
    var localRot1: Quaterniond=Quaterniond(),
    var maxTorque: Double=0.0
): BaseConstraintData(shipId0, shipId1, compliance, constraintType)

object VSConstraintDeserializationUtil {
    private fun makeConstraintData(cTag: CompoundTag): BaseConstraintData? {
        if (!cTag.contains("constraintType")) {return null}
        val type = try { valueOf(cTag.getString("constraintType")) } catch (e: Exception) { return null }

        return when (type) {
            ROPE               -> ForceConstraintData()
            SLIDE              -> ForceConstraintData()
            ATTACHMENT         -> ForceConstraintData()
            POS_DAMPING        -> ForceConstraintData()

            ROT_DAMPING        -> TorqueConstraintData()
            FIXED_ORIENTATION  -> TorqueConstraintData()
            HINGE_ORIENTATION  -> TorqueConstraintData()
            HINGE_SWING_LIMITS -> TorqueConstraintData()
            HINGE_TARGET_ANGLE -> TorqueConstraintData()
            SPHERICAL_SWING_LIMITS -> TorqueConstraintData()
            SPHERICAL_TWIST_LIMITS -> TorqueConstraintData()

            FIXED_ATTACHMENT_ORIENTATION -> null
            else -> {LOG("UNKNOWN VS CONSTRAINT TYPE ${type}"); null}
        }
    }

    private fun loadBaseData(cTag: CompoundTag, cdata: BaseConstraintData): Boolean {
        if (!( cTag.contains("shipId0")
            && cTag.contains("shipId1")
            && cTag.contains("compliance")
            && cTag.contains("constraintType")
            )) { return false }
        cdata.shipId0 = cTag.getLong("shipId0")
        cdata.shipId1 = cTag.getLong("shipId1")
        cdata.compliance = cTag.getDouble("compliance")
        cdata.constraintType = VSConstraintType.valueOf(cTag.getString("constraintType"))

        return true
    }

    private fun loadForceData(cTag: CompoundTag, cdata: ForceConstraintData): Boolean {
        if (!cTag.contains("maxForce")) {return false}

        cdata.localPos0 = cTag.getVector3d("localPos0") ?: return false
        cdata.localPos1 = cTag.getVector3d("localPos1") ?: return false
        cdata.maxForce = cTag.getDouble("maxForce")

        return true
    }

    private fun loadTorqueData(cTag: CompoundTag, cdata: TorqueConstraintData): Boolean {
        if ( !cTag.contains("maxTorque") ) {return false}

        cdata.localRot0 = cTag.getQuaterniond("localRot0") ?: return false
        cdata.localRot1 = cTag.getQuaterniond("localRot1") ?: return false
        cdata.maxTorque = cTag.getDouble("maxTorque")

        return true
    }

    private fun makeConstraint(cTag: CompoundTag, cdata: BaseConstraintData): VSConstraint? {
        try {
            return when (cdata.constraintType) {
                ROPE               -> { cdata as ForceConstraintData; VSRopeConstraint      (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("ropeLength")) }
                SLIDE              -> { cdata as ForceConstraintData; VSSlideConstraint     (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getVector3d("localSlideAxis0")!!, cTag.getDouble("maxDistBetweenPoints")) }
                ATTACHMENT         -> { cdata as ForceConstraintData; VSAttachmentConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("fixedDistance")) }
                POS_DAMPING        -> { cdata as ForceConstraintData; VSPosDampingConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("posDamping")) }

                ROT_DAMPING            -> { cdata as TorqueConstraintData; VSRotDampingConstraint      (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("rotDamping"), VSRotDampingAxes.valueOf(cTag.getString("rotDampingAxes")))}
                FIXED_ORIENTATION      -> { cdata as TorqueConstraintData; VSFixedOrientationConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque)}
                HINGE_ORIENTATION      -> { cdata as TorqueConstraintData; VSHingeOrientationConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque)}
                HINGE_SWING_LIMITS     -> { cdata as TorqueConstraintData; VSHingeSwingLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minSwingAngle"), cTag.getDouble("maxSwingAngle"))}
                HINGE_TARGET_ANGLE     -> { cdata as TorqueConstraintData; VSHingeTargetAngleConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("targetAngle"), cTag.getDouble("nextTickTargetAngle"))}
                SPHERICAL_SWING_LIMITS -> { cdata as TorqueConstraintData; VSSphericalSwingLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minSwingAngle"), cTag.getDouble("maxSwingAngle"))}
                SPHERICAL_TWIST_LIMITS -> { cdata as TorqueConstraintData; VSSphericalTwistLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minTwistAngle"), cTag.getDouble("maxTwistAngle"))}

                FIXED_ATTACHMENT_ORIENTATION -> null
                else -> {LOG("UNKNOWN VS CONSTRAINT TYPE ${cdata.constraintType}"); null}
            }
        } catch (e: Exception) {
            LOG("SMTH WENT WRONG, NOT DESERIALIZING TAG ${cTag}")
            return null
        }
    }

    fun deserializeConstraint(cTag: CompoundTag): VSConstraint? {
        val cData = makeConstraintData(cTag) ?: return null
        if (!loadBaseData(cTag, cData)) {return null}
        if (!when(cData) {
            is ForceConstraintData  -> loadForceData(cTag, cData)
            is TorqueConstraintData -> loadTorqueData(cTag, cData)
            else -> throw RuntimeException("IMPOSSIBLE SITUATION. UNKNOWN CONSTRAINT DATA TYPE.")
        }) {return null}

        return makeConstraint(cTag, cData)
    }
}