package net.spaceeye.vmod.constraintsManaging

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSForceConstraint
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import java.lang.AssertionError

//it's needed to not use ship's chunk claim
inline fun getCenterPos(x: Int, z: Int) = Vector3d(((x / 16 / 256 - 1) * 256 + 128) * 16, 0, ((z / 16 / 256) * 256 + 128) * 16)

fun commonCopy(
        level: ServerLevel,
        mapped: Map<ShipId, ShipId>,
        constraint: VSForceConstraint,
        attachmentPoints_: List<BlockPos>,
        renderer: BaseRenderer?,
        constructor: (nShip1Id: ShipId, nShip2Id: ShipId, nShip1: ServerShip?, nShip2: ServerShip?, localPos0: Vector3d, localPos1: Vector3d, newAttachmentPoints: List<BlockPos>, newRenderer: BaseRenderer?) -> MConstraint?): MConstraint? {
    if (!mapped.keys.containsAll(listOf(constraint.shipId0, constraint.shipId1))) {return null}

    val inShipyard1 = level.isChunkInShipyard(constraint.localPos0.x().toInt() / 16, constraint.localPos0.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(constraint.localPos1.x().toInt() / 16, constraint.localPos1.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId1]!!) else null

    // not actually center positions, but positions are transformed the same way, so it doesn't matter
    val oCentered1 = if (inShipyard1) {getCenterPos(constraint.localPos0.x().toInt(), constraint.localPos0.z().toInt())} else {null}
    val oCentered2 = if (inShipyard2) {getCenterPos(constraint.localPos1.x().toInt(), constraint.localPos1.z().toInt())} else {null}
    val nCentered1 = if (nShip1!=null){getCenterPos(nShip1.transform.positionInShip.x().toInt(), nShip1.transform.positionInShip.z().toInt())} else {null}
    val nCentered2 = if (nShip2!=null){getCenterPos(nShip2.transform.positionInShip.x().toInt(), nShip2.transform.positionInShip.z().toInt())} else {null}

    val nShip1Id = nShip1?.id ?: constraint.shipId0
    val nShip2Id = nShip2?.id ?: constraint.shipId1

    val localPos0 = if (nShip1 != null) Vector3d(constraint.localPos0) - oCentered1!! + nCentered1!! else Vector3d(constraint.localPos0)
    val localPos1 = if (nShip2 != null) Vector3d(constraint.localPos1) - oCentered2!! + nCentered2!! else Vector3d(constraint.localPos1)

    val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints_[0]) + 0.5 - oCentered1!! + nCentered1!! else Vector3d(attachmentPoints_[0])).toBlockPos()
    val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints_[1]) + 0.5 - oCentered2!! + nCentered2!! else Vector3d(attachmentPoints_[1])).toBlockPos()

    val newAttachmentPoints = listOf(apoint1, apoint2)
    val newRenderer = renderer?.copy(nShip1, nShip2, localPos0, localPos1)

    return constructor(nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer)
}