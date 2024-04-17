package net.spaceeye.vmod.constraintsManaging

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.vs.DummyServerShip
import net.spaceeye.vmod.transformProviders.FixedPositionTransformProvider
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSForceConstraint
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import java.lang.AssertionError

fun commonCopy(
        level: ServerLevel,
        mapped: Map<ShipId, ShipId>,
        constraint: VSForceConstraint,
        attachmentPoints_: List<BlockPos>,
        renderer: BaseRenderer?,
        constructor: (nShip1Id: ShipId, nShip2Id: ShipId, nShip1: ServerShip?, nShip2: ServerShip?, localPos0: Vector3d, localPos1: Vector3d, newAttachmentPoints: List<BlockPos>, newRenderer: BaseRenderer?) -> MConstraint?): MConstraint? {
    if (!mapped.keys.containsAll(listOf(constraint.shipId0, constraint.shipId1))) { return null }

    val inShipyard1 = level.isChunkInShipyard(constraint.localPos0.x().toInt() / 16, constraint.localPos0.z().toInt() / 16)
    val inShipyard2 = level.isChunkInShipyard(constraint.localPos1.x().toInt() / 16, constraint.localPos1.z().toInt() / 16)

    val nShip1 = if (inShipyard1) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId0]!!) else null
    val nShip2 = if (inShipyard2) level.shipObjectWorld.allShips.getById(mapped[constraint.shipId1]!!) else null

    //TODO extremely sus math
    val oCentered1 = if (inShipyard1) {Vector3d(((constraint.localPos0.x().toInt() / 16 / 256 - 1) * 256 + 128) * 16, 128.5, ((constraint.localPos0.z().toInt() / 16 / 256) * 256 + 128) * 16)} else {null}
    val oCentered2 = if (inShipyard2) {Vector3d(((constraint.localPos1.x().toInt() / 16 / 256 - 1) * 256 + 128) * 16, 128.5, ((constraint.localPos1.z().toInt() / 16 / 256) * 256 + 128) * 16)} else {null}
    val nCentered1 = if (nShip1 != null) {Vector3d(nShip1.chunkClaim.xMiddle * 16, 128.5, nShip1.chunkClaim.zMiddle * 16)} else {null}
    val nCentered2 = if (nShip2 != null) {Vector3d(nShip2.chunkClaim.xMiddle * 16, 128.5, nShip2.chunkClaim.zMiddle * 16)} else {null}

    val nShip1Id = nShip1?.id ?: constraint.shipId0
    val nShip2Id = nShip2?.id ?: constraint.shipId1

    val localPos0 = if (nShip1 != null) Vector3d(constraint.localPos0) - oCentered1!! + nCentered1!! else Vector3d(constraint.localPos0)
    val localPos1 = if (nShip2 != null) Vector3d(constraint.localPos1) - oCentered2!! + nCentered2!! else Vector3d(constraint.localPos1)

    val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints_[0]) + 0.5 - oCentered1!! + nCentered1!! else Vector3d(attachmentPoints_[0])).toBlockPos()
    val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints_[1]) + 0.5 - oCentered2!! + nCentered2!! else Vector3d(attachmentPoints_[1])).toBlockPos()

    val newAttachmentPoints = listOf(apoint1, apoint2)

    var newRenderer: BaseRenderer? = null
    if (renderer != null) {
        newRenderer = when (renderer) {
            is A2BRenderer  -> { A2BRenderer (nShip1 != null, nShip2 != null, localPos0, localPos1, renderer.color, renderer.width) }
            is RopeRenderer -> { RopeRenderer(nShip1 != null, nShip2 != null, localPos0, localPos1, renderer.length, renderer.width, renderer.segments) }
            else -> {throw AssertionError() }
        }
    }

    val cShip1 = if (nShip1 != null) { DummyServerShip(nShip1) } else null
    val cShip2 = if (nShip2 != null) { DummyServerShip(nShip2) } else null

    if (cShip1 != null) {cShip1.transform = (cShip1.transform as ShipTransformImpl).copy(positionInShip = (nShip1!!.transformProvider as FixedPositionTransformProvider).positionInShip)}
    if (cShip2 != null) {cShip2.transform = (cShip2.transform as ShipTransformImpl).copy(positionInShip = (nShip2!!.transformProvider as FixedPositionTransformProvider).positionInShip)}

    return constructor(nShip1Id, nShip2Id, cShip1, cShip2, localPos0, localPos1, newAttachmentPoints, newRenderer)
}