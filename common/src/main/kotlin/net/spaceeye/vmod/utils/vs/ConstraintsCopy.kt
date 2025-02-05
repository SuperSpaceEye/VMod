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

//it's needed to not use ship's chunk claim
//TODO rework to use arbitrary centers
inline fun getCenterPos(x: Int, z: Int) = Vector3d(((x / 16 / 256 - 1) * 256 + 128) * 16, 0, ((z / 16 / 256) * 256 + 128) * 16)
inline fun getCenterPos(pos: Vector3d): Vector3d = getCenterPos(pos.x.toInt(), pos.z.toInt())
inline fun getCenterPos(pos:  org.joml.Vector3dc): Vector3d = getCenterPos(pos.x().toInt(), pos.z().toInt())

inline fun updatePosition(old: Vector3d, newShip: Ship): Vector3d = old - Vector3d(getCenterPos(old.x.toInt(), old.z.toInt())) + Vector3d(getCenterPos(newShip.transform.positionInShip.x().toInt(), newShip.transform.positionInShip.z().toInt()))

fun tryMovePosition(pos: Vector3d, shipId: Long, level: ServerLevel, mapped: Map<ShipId, ShipId>): Vector3d? {
    if (!mapped.containsKey(shipId)) {return null}

    if (!level.isBlockInShipyard(pos.x, pos.y, pos.z)) {return pos.copy()}

    val newShip = level.shipObjectWorld.allShips.getById(mapped[shipId]!!)!!
    val oldCenter = getCenterPos(pos)
    val newCenter = getCenterPos(newShip.transform.positionInShip)
    val newPos = pos - oldCenter + newCenter
    return newPos
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