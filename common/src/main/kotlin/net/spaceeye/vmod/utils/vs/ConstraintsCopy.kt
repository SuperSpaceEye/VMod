package net.spaceeye.vmod.utils.vs

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld

//it's needed to not use ship's chunk claim
//TODO rework to use arbitrary centers
inline fun getCenterPos(x: Int, z: Int) = Vector3d(((x / 16 / 256 - 1) * 256 + 128) * 16, 0, ((z / 16 / 256) * 256 + 128) * 16)
inline fun getCenterPos(pos: Vector3d): Vector3d = getCenterPos(pos.x.toInt(), pos.z.toInt())
inline fun getCenterPos(pos:  Vector3dc): Vector3d = getCenterPos(pos.x().toInt(), pos.z().toInt())

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
