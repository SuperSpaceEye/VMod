package net.spaceeye.vmod.utils.vs

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.getMapper
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.mod.common.shipObjectWorld

//private open class BaseConstraintData(
//    var shipId0: ShipId,
//    var shipId1: ShipId,
//    var compliance: Double,
//    var constraintType: VSJointType,
//)
//
//private open class ForceConstraintData(
//    shipId0: ShipId=0,
//    shipId1: ShipId=0,
//    compliance: Double=0.0,
//    constraintType: VSJointType=ATTACHMENT,
//    var localPos0: Vector3dc=Vector3d(),
//    var localPos1: Vector3dc=Vector3d(),
//    var maxForce: Double=0.0
//): BaseConstraintData(shipId0, shipId1, compliance, constraintType)
//
//private open class TorqueConstraintData(
//    shipId0: ShipId=0,
//    shipId1: ShipId=0,
//    compliance: Double=0.0,
//    constraintType: VSJointType=ATTACHMENT,
//    var localRot0: Quaterniond=Quaterniond(),
//    var localRot1: Quaterniond=Quaterniond(),
//    var maxTorque: Double=0.0
//): BaseConstraintData(shipId0, shipId1, compliance, constraintType)

object VSJointDeserializationUtil {
//    private fun makeConstraintData(cTag: CompoundTag): BaseConstraintData? {
//        if (!cTag.contains("constraintType")) {return null}
//        val type = try { valueOf(cTag.getString("constraintType")) } catch (e: Exception) { return null }
//
//        return when (type) {
//            ROPE               -> ForceConstraintData()
//            SLIDE              -> ForceConstraintData()
//            ATTACHMENT         -> ForceConstraintData()
//            POS_DAMPING        -> ForceConstraintData()
//
//            ROT_DAMPING        -> TorqueConstraintData()
//            FIXED_ORIENTATION  -> TorqueConstraintData()
//            HINGE_ORIENTATION  -> TorqueConstraintData()
//            HINGE_SWING_LIMITS -> TorqueConstraintData()
//            HINGE_TARGET_ANGLE -> TorqueConstraintData()
//            SPHERICAL_SWING_LIMITS -> TorqueConstraintData()
//            SPHERICAL_TWIST_LIMITS -> TorqueConstraintData()
//
//            FIXED_ATTACHMENT_ORIENTATION -> null
//            else -> {WLOG("Unknown VS constraint type $type"); null}
//        }
//    }
//
//    private fun loadBaseData(cTag: CompoundTag, cdata: BaseConstraintData): Boolean {
//        if (!( cTag.contains("shipId0")
//            && cTag.contains("shipId1")
//            && cTag.contains("compliance")
//            && cTag.contains("constraintType")
//            )) { return false }
//        cdata.shipId0 = cTag.getLong("shipId0")
//        cdata.shipId1 = cTag.getLong("shipId1")
//        cdata.compliance = cTag.getDouble("compliance")
//        cdata.constraintType = VSJointType.valueOf(cTag.getString("constraintType"))
//
//        return true
//    }
//
//    private fun loadForceData(cTag: CompoundTag, cdata: ForceConstraintData): Boolean {
//        if (!cTag.contains("maxForce")) {return false}
//
//        cdata.localPos0 = cTag.getVector3d("localPos0") ?: return false
//        cdata.localPos1 = cTag.getVector3d("localPos1") ?: return false
//        cdata.maxForce = cTag.getDouble("maxForce")
//
//        return true
//    }
//
//    private fun loadTorqueData(cTag: CompoundTag, cdata: TorqueConstraintData): Boolean {
//        if ( !cTag.contains("maxTorque") ) {return false}
//
//        cdata.localRot0 = cTag.getQuaterniond("localRot0") ?: return false
//        cdata.localRot1 = cTag.getQuaterniond("localRot1") ?: return false
//        cdata.maxTorque = cTag.getDouble("maxTorque")
//
//        return true
//    }
//
//    private fun makeConstraint(cTag: CompoundTag, cdata: BaseConstraintData): VSJoint? {
//        try {
//            return when (cdata.constraintType) {
//                ROPE               -> { cdata as ForceConstraintData; VSRopeConstraint      (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("ropeLength")) }
//                SLIDE              -> { cdata as ForceConstraintData; VSSlideConstraint     (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getVector3d("localSlideAxis0")!!, cTag.getDouble("maxDistBetweenPoints")) }
//                ATTACHMENT         -> { cdata as ForceConstraintData; VSAttachmentConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("fixedDistance")) }
//                POS_DAMPING        -> { cdata as ForceConstraintData; VSPosDampingConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localPos0, cdata.localPos1, cdata.maxForce, cTag.getDouble("posDamping")) }
//
//                ROT_DAMPING            -> { cdata as TorqueConstraintData; VSRotDampingConstraint      (cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("rotDamping"), VSRotDampingAxes.valueOf(cTag.getString("rotDampingAxes")))}
//                FIXED_ORIENTATION      -> { cdata as TorqueConstraintData; VSFixedOrientationConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque)}
//                HINGE_ORIENTATION      -> { cdata as TorqueConstraintData; VSHingeOrientationConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque)}
//                HINGE_SWING_LIMITS     -> { cdata as TorqueConstraintData; VSHingeSwingLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minSwingAngle"), cTag.getDouble("maxSwingAngle"))}
//                HINGE_TARGET_ANGLE     -> { cdata as TorqueConstraintData; VSHingeTargetAngleConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("targetAngle"), cTag.getDouble("nextTickTargetAngle"))}
//                SPHERICAL_SWING_LIMITS -> { cdata as TorqueConstraintData; VSSphericalSwingLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minSwingAngle"), cTag.getDouble("maxSwingAngle"))}
//                SPHERICAL_TWIST_LIMITS -> { cdata as TorqueConstraintData; VSSphericalTwistLimitsConstraint(cdata.shipId0, cdata.shipId1, cdata.compliance, cdata.localRot0, cdata.localRot1, cdata.maxTorque, cTag.getDouble("minTwistAngle"), cTag.getDouble("maxTwistAngle"))}
//
//                FIXED_ATTACHMENT_ORIENTATION -> null
//                else -> {WLOG("Unknown VS constraint type ${cdata.constraintType}"); null}
//            }
//        } catch (e: Exception) {
//            WLOG("An exception has occurred, not deserializing tag ${cTag}\n${e.stackTraceToString()}")
//            return null
//        }
//    }

    fun deserializeConstraint(cTag: CompoundTag): VSJoint? {
//        val cData = makeConstraintData(cTag) ?: return null
//        if (!loadBaseData(cTag, cData)) {return null}
//        if (!when(cData) {
//            is ForceConstraintData -> loadForceData(cTag, cData)
//            is TorqueConstraintData -> loadTorqueData(cTag, cData)
//            else -> throw RuntimeException("IMPOSSIBLE SITUATION. UNKNOWN CONSTRAINT DATA TYPE.")
//        }) {return null}
//
//        return makeConstraint(cTag, cData)

        val mapper = getMapper()

        val shipId0 = cTag.getLong("shipId0")
        val shipId1 = cTag.getLong("shipId1")

        val joint = try {
            when(VSJointType.valueOf(cTag.getString("type"))) {
                VSJointType.FIXED     -> mapper.readValue(cTag.getByteArray("constraint"), VSFixedJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.DISTANCE  -> mapper.readValue(cTag.getByteArray("constraint"), VSDistanceJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.PRISMATIC -> mapper.readValue(cTag.getByteArray("constraint"), VSPrismaticJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.SPHERICAL -> mapper.readValue(cTag.getByteArray("constraint"), VSSphericalJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.REVOLUTE  -> mapper.readValue(cTag.getByteArray("constraint"), VSRevoluteJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.GEAR      -> mapper.readValue(cTag.getByteArray("constraint"), VSGearJoint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.D6        -> mapper.readValue(cTag.getByteArray("constraint"), VSD6Joint::class.java).copy(shipId0, shipId1 = shipId1)
                VSJointType.RACK_AND_PINION -> mapper.readValue(cTag.getByteArray("constraint"), VSRackAndPinionJoint::class.java).copy(shipId0, shipId1 = shipId1)
            }
        } catch (e: Exception) {
            println(e.stackTraceToString()); null
        } catch (e: Error) {
            println(e.stackTraceToString()); null}

        return joint
    }

    //TODO this should probably be changed somehow
    fun tryConvertDimensionId(tag: CompoundTag, lastDimensionIds: Map<Long, String>) {
        if (!tag.contains("shipId1") || !tag.contains("shipId0")) {return}

        tag.getLong("shipId0")?.let { id ->
            val dimensionIdStr = lastDimensionIds[id] ?: return@let
            tag.putLong("shipId0", ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable[dimensionIdStr]!!)
        }

        tag.getLong("shipId1")?.let { id ->
            val dimensionIdStr = lastDimensionIds[id] ?: return@let
            tag.putLong("shipId1", ServerLevelHolder.overworldServerLevel!!.shipObjectWorld.dimensionToGroundBodyIdImmutable[dimensionIdStr]!!)
        }
    }
}