package net.spaceeye.vmod.utils.vs

import org.joml.Quaterniondc
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*

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