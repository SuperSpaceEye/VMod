package net.spaceeye.vmod.utils.vs

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniondc
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld

fun VSForceConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null): VSForceConstraint {
    return when (this) {
        is VSAttachmentConstraint -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localPos0_?:localPos0,localPos1_?:localPos1,maxForce_?:maxForce)
        is VSPosDampingConstraint -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localPos0_?:localPos0,localPos1_?:localPos1,maxForce_?:maxForce)
        is VSRopeConstraint       -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localPos0_?:localPos0,localPos1_?:localPos1,maxForce_?:maxForce)
        is VSSlideConstraint      -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localPos0_?:localPos0,localPos1_?:localPos1,maxForce_?:maxForce)
        else -> throw AssertionError("Impossible")
    }
}

fun VSTorqueConstraint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null): VSTorqueConstraint {
    return when (this) {
        is VSFixedOrientationConstraint     -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSHingeOrientationConstraint     -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSHingeSwingLimitsConstraint     -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSHingeTargetAngleConstraint     -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSSphericalSwingLimitsConstraint -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSSphericalTwistLimitsConstraint -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        is VSRotDampingConstraint           -> copy(shipId0_?:shipId0,shipId1_?:shipId1,compliance_?:compliance,localRot0_?:localRot0,localRot1_?:localRot1,maxTorque_?:maxTorque)
        else -> throw AssertionError("Impossible")
    }
}

inline fun <T: VSForceConstraint> T.copyT(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null, maxForce_: Double? = null): T? {
    return this.copy(shipId0_, shipId1_, compliance_, localPos0_, localPos1_, maxForce_) as? T
}

inline fun <T: VSTorqueConstraint> T.copyT(shipId0_: ShipId? = null, shipId1_: ShipId? = null, compliance_: Double? = null, localRot0_: Quaterniondc? = null, localRot1_: Quaterniondc? = null, maxTorque_: Double? = null): T? {
    return this.copy(shipId0_, shipId1_, compliance_, localRot0_, localRot1_, maxTorque_) as? T
}

//it's needed to not use ship's chunk claim
inline fun getCenterPos(x: Int, z: Int) = Vector3d(((x / 16 / 256 - 1) * 256 + 128) * 16, 0, ((z / 16 / 256) * 256 + 128) * 16)
inline fun updatePosition(old: Vector3d, newShip: Ship): Vector3d = old - Vector3d(getCenterPos(old.x.toInt(), old.z.toInt())) + Vector3d(getCenterPos(newShip.transform.positionInShip.x().toInt(), newShip.transform.positionInShip.z().toInt()))

fun <T: VSForceConstraint> T.copy(level: ServerLevel, mapped: Map<ShipId, ShipId>): T? {
    if (!mapped.keys.containsAll(listOf(this.shipId0, this.shipId1))) {return null}

    val inShipyard1 = level.isChunkInShipyard(this.localPos0.x().toInt() / 16, this.localPos0.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(this.localPos1.x().toInt() / 16, this.localPos1.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[this.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[this.shipId1]!!) else null

    val oCentered1 = if (inShipyard1) {getCenterPos(this.localPos0.x().toInt(), this.localPos0.z().toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(this.localPos1.x().toInt(), this.localPos1.z().toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}

    val nShip1Id = nShip1?.id ?: this.shipId0
    val nShip2Id = nShip2?.id ?: this.shipId1

    val localPos0 = (if (nShip1 != null) (Vector3d(this.localPos0) - oCentered1!! + nCentered1!!).toJomlVector3d() else org.joml.Vector3d(this.localPos0))
    val localPos1 = (if (nShip2 != null) (Vector3d(this.localPos1) - oCentered2!! + nCentered2!!).toJomlVector3d() else org.joml.Vector3d(this.localPos1))

    return this.copy(nShip1Id, nShip2Id, compliance, localPos0, localPos1) as? T
}

fun <T: VSTorqueConstraint> T.copy(mapped: Map<ShipId, ShipId>): T? {
    return this.copy(mapped[this.shipId0] ?: return null, mapped[this.shipId1] ?: return null) as? T
}

fun copyAttachmentPoints(constraint: VSForceConstraint, attachmentPoints: List<BlockPos>, level: ServerLevel, mapped: Map<ShipId, ShipId>): MutableList<BlockPos> {
    val inShipyard1 = level.isChunkInShipyard(constraint.localPos0.x().toInt() / 16, constraint.localPos0.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(constraint.localPos1.x().toInt() / 16, constraint.localPos1.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId1]!!) else null


    val oCentered1 = if (inShipyard1) {getCenterPos(constraint.localPos0.x().toInt(), constraint.localPos0.z().toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(constraint.localPos1.x().toInt(), constraint.localPos1.z().toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}


    val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints[0]) + 0.5 - oCentered1!! + nCentered1!! else Vector3d(attachmentPoints[0])).toBlockPos()
    val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints[1]) + 0.5 - oCentered2!! + nCentered2!! else Vector3d(attachmentPoints[1])).toBlockPos()

    val newAttachmentPoints = mutableListOf(apoint1, apoint2)

    return newAttachmentPoints
}