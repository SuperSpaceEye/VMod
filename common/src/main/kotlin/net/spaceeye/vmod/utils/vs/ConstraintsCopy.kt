package net.spaceeye.vmod.utils.vs

import org.joml.Quaterniondc
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*


fun VSAttachmentConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null, fixedDistance_:   Double?    = null) = VSAttachmentConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localPos0_ ?: localPos0, localPos1_ ?: localPos1, maxForce_ ?: maxForce, fixedDistance_ ?: fixedDistance)
fun VSPosDampingConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null, posDamping_:      Double?    = null) = VSPosDampingConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localPos0_ ?: localPos0, localPos1_ ?: localPos1, maxForce_ ?: maxForce, posDamping_ ?: posDamping)
fun VSRopeConstraint      .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null, ropeLength_:      Double?    = null) = VSPosDampingConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localPos0_ ?: localPos0, localPos1_ ?: localPos1, maxForce_ ?: maxForce, ropeLength_ ?: ropeLength)
fun VSSlideConstraint     .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null, localSlideAxis0_: Vector3dc? = null, maxDistBetweenPoints_: Double? = null) = VSSlideConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localPos0_ ?: localPos0, localPos1_ ?: localPos1, maxForce_ ?: maxForce, localSlideAxis0_ ?: localSlideAxis0, maxDistBetweenPoints_ ?: maxDistBetweenPoints)

fun VSFixedOrientationConstraint    .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null)                                                                   = VSFixedOrientationConstraint(    shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque)
fun VSHingeOrientationConstraint    .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null)                                                                   = VSHingeOrientationConstraint(    shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque)
fun VSHingeSwingLimitsConstraint    .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null, minSwingAngle_: Double? = null, maxSwingAngle_:   Double? = null) = VSHingeSwingLimitsConstraint(    shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque, minSwingAngle_ ?: minSwingAngle, maxSwingAngle_ ?: maxSwingAngle)
fun VSHingeTargetAngleConstraint    .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null, targetAngle_:   Double? = null, nextTargetAngle_: Double? = null) = VSHingeTargetAngleConstraint(    shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque, targetAngle_ ?: targetAngle, nextTargetAngle_ ?: nextTickTargetAngle)
fun VSSphericalSwingLimitsConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null, minSwingAngle_: Double? = null, maxSwingAngle_:   Double? = null) = VSSphericalSwingLimitsConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque, minSwingAngle_ ?: minSwingAngle, maxSwingAngle_ ?: maxSwingAngle)
fun VSSphericalTwistLimitsConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null, minTwistAngle_: Double? = null, maxTwistAngle_:   Double? = null) = VSSphericalTwistLimitsConstraint(shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque, minTwistAngle_ ?: minTwistAngle, maxTwistAngle_ ?: maxTwistAngle)
fun VSRotDampingConstraint          .copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null, rotDamping_:    Double? = null, rotDampingAxes_: VSRotDampingAxes? = null) = VSRotDampingConstraint( shipId0_ ?: shipId0, shipId1_ ?: shipId1, compliance_ ?: compliance, localRot0_ ?: localRot0, localRot1_ ?: localRot1, maxTorque_ ?: maxTorque, rotDamping_ ?: rotDamping, rotDampingAxes_ ?: rotDampingAxes)

fun VSForceConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null): VSForceConstraint {
    return when (this) {
        is VSAttachmentConstraint -> this.copy(shipId0_, shipId1_, compliance_, localPos0_, localPos1_, maxForce_)
        is VSPosDampingConstraint -> this.copy(shipId0_, shipId1_, compliance_, localPos0_, localPos1_, maxForce_)
        is VSRopeConstraint       -> this.copy(shipId0_, shipId1_, compliance_, localPos0_, localPos1_, maxForce_)
        is VSSlideConstraint      -> this.copy(shipId0_, shipId1_, compliance_, localPos0_, localPos1_, maxForce_)
        else -> throw AssertionError("Impossible")
    }
}

fun VSTorqueConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null): VSTorqueConstraint {
    return when (this) {
        is VSFixedOrientationConstraint     -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSHingeOrientationConstraint     -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSHingeSwingLimitsConstraint     -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSHingeTargetAngleConstraint     -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSSphericalSwingLimitsConstraint -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSSphericalTwistLimitsConstraint -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        is VSRotDampingConstraint           -> this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_)
        else -> throw AssertionError("Impossible")
    }
}