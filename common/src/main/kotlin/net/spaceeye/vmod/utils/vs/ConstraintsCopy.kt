package net.spaceeye.vmod.utils.vs

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld

fun VSJoint.copy(shipId0_: ShipId? = null, shipId1_: ShipId? = null, localPos0_: Vector3dc? = null, localPos1_: Vector3dc? = null): VSJoint {
    return when (this) {
        is VSD6Joint            -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSDistanceJoint      -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSFixedJoint         -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSGearJoint          -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSPrismaticJoint     -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSRackAndPinionJoint -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSRevoluteJoint      -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        is VSSphericalJoint     -> this.copy(shipId0_ ?: shipId0, localPos0_?.let { VSJointPose(it, pose0.rot) } ?: pose0, shipId1_ ?: shipId1, localPos1_?.let { VSJointPose(it, pose1.rot) } ?: pose1)
        else -> throw AssertionError("Type ${this.javaClass.name} can't be handled which shouldn't happen but if it does make an issue on https://github.com/SuperSpaceEye/VMod")
    }
}

//it's needed to not use ship's chunk claim
inline fun getCenterPos(x: Int, z: Int) = Vector3d(((x / 16 / 256 - 1) * 256 + 128) * 16, 0, ((z / 16 / 256) * 256 + 128) * 16)
inline fun getCenterPos(pos: Vector3d): Vector3d = getCenterPos(pos.x.toInt(), pos.z.toInt())
inline fun getCenterPos(pos:  org.joml.Vector3dc): Vector3d = getCenterPos(pos.x().toInt(), pos.z().toInt())

inline fun updatePosition(old: Vector3d, newShip: Ship): Vector3d = old - Vector3d(getCenterPos(old.x.toInt(), old.z.toInt())) + Vector3d(getCenterPos(newShip.transform.positionInShip.x().toInt(), newShip.transform.positionInShip.z().toInt()))

fun <T: VSJoint> T.copy(level: ServerLevel, mapped: Map<ShipId, ShipId>): T? {
    if (!mapped.keys.containsAll(listOf(this.shipId0, this.shipId1))) {return null}

    val inShipyard1 = level.isChunkInShipyard(this.pose0.pos.x().toInt() / 16, this.pose0.pos.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(this.pose1.pos.x().toInt() / 16, this.pose1.pos.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[this.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[this.shipId1]!!) else null

    val oCentered1 = if (inShipyard1) {getCenterPos(this.pose0.pos.x().toInt(), this.pose0.pos.z().toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(this.pose1.pos.x().toInt(), this.pose1.pos.z().toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}

    val nShip1Id = nShip1?.id ?: this.shipId0
    val nShip2Id = nShip2?.id ?: this.shipId1

    val localPos0 = (if (nShip1 != null) (Vector3d(this.pose0.pos) - oCentered1!! + nCentered1!!).toJomlVector3d() else org.joml.Vector3d(this.pose0.pos))
    val localPos1 = (if (nShip2 != null) (Vector3d(this.pose1.pos) - oCentered2!! + nCentered2!!).toJomlVector3d() else org.joml.Vector3d(this.pose1.pos))

    return this.copy(nShip1Id, nShip2Id, localPos0, localPos1) as T
}

fun tryMovePosition(pos: Vector3d, shipId: Long, level: ServerLevel, mapped: Map<ShipId, ShipId>): Vector3d? {
    if (!mapped.containsKey(shipId)) {return null}

    if (!level.isBlockInShipyard(pos.x, pos.y, pos.z)) {return pos.copy()}

    val newShip = level.shipObjectWorld.allShips.getById(mapped[shipId]!!)!!
    val oldCenter = getCenterPos(pos)
    val newCenter = getCenterPos(newShip.transform.positionInShip)
    val newPos = pos - oldCenter + newCenter
    return newPos
}

fun copyAttachmentPoints(constraint: VSJoint, attachmentPoints: List<BlockPos>, level: ServerLevel, mapped: Map<ShipId, ShipId>): MutableList<BlockPos> {
    val inShipyard1 = level.isChunkInShipyard(constraint.pose0.pos.x().toInt() / 16, constraint.pose0.pos.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(constraint.pose1.pos.x().toInt() / 16, constraint.pose1.pos.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId1]!!) else null


    val oCentered1 = if (inShipyard1) {getCenterPos(constraint.pose0.pos.x().toInt(), constraint.pose0.pos.z().toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(constraint.pose1.pos.x().toInt(), constraint.pose1.pos.z().toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}


    val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints[0]) + 0.5 - oCentered1!! + nCentered1!! else Vector3d(attachmentPoints[0])).toBlockPos()
    val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints[1]) + 0.5 - oCentered2!! + nCentered2!! else Vector3d(attachmentPoints[1])).toBlockPos()

    val newAttachmentPoints = mutableListOf(apoint1, apoint2)

    return newAttachmentPoints
}

fun copyAttachmentPoints(sPos1: Vector3d, sPos2: Vector3d, shipId1: Long, shipId2: Long, attachmentPoints: List<BlockPos>, level: ServerLevel, mapped: Map<ShipId, ShipId>): MutableList<BlockPos> {
    val inShipyard1 = level.isChunkInShipyard(sPos1.x.toInt() / 16, sPos1.z.toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(sPos2.x.toInt() / 16, sPos2.z.toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[shipId1]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[shipId2]!!) else null


    val oCentered1 = if (inShipyard1) {getCenterPos(sPos1.x.toInt(), sPos1.z.toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(sPos2.x.toInt(), sPos2.z.toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}


    val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints[0]) + 0.5 - oCentered1!! + nCentered1!! else Vector3d(attachmentPoints[0])).toBlockPos()
    val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints[1]) + 0.5 - oCentered2!! + nCentered2!! else Vector3d(attachmentPoints[1])).toBlockPos()

    val newAttachmentPoints = mutableListOf(apoint1, apoint2)

    return newAttachmentPoints
}