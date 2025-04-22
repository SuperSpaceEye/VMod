package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.JVector3dc
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId

inline fun updatePosition(old: Vector3d, oldCenter: Vector3d, newCenter: Vector3d): Vector3d = old - oldCenter + newCenter

fun tryMovePosition(pos: Vector3d, shipId: Long, mapped: Map<ShipId, Pair<Vector3d, Vector3d>>): Vector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos - oldCenter + newCenter
}

fun tryMovePosition(pos: Vector3dc, shipId: Long, mapped: Map<ShipId, Pair<Vector3dc, Vector3dc>>): JVector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos.sub(oldCenter, JVector3d()).add(newCenter)
}

fun tryMovePositionJ(pos: Vector3d, shipId: Long, mapped: Map<ShipId, Pair<JVector3d, JVector3dc>>): Vector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos.sub(oldCenter.x, oldCenter.y, oldCenter.z).add(newCenter.x(), newCenter.y(), newCenter.z())
}
