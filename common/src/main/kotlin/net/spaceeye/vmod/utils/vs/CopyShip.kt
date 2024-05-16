package net.spaceeye.vmod.utils.vs

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.constraintsManaging.VSConstraintsTracker
import net.spaceeye.vmod.constraintsManaging.getManagedConstraint
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.transformProviders.FixedPositionTransformProvider
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.networking.simple.sendToClient
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates
import org.valkyrienskies.mod.common.util.toJOML

val AIR: BlockState = Blocks.AIR.defaultBlockState()

private fun copyShipBlock(level: ServerLevel, state: BlockState, ox: Int, oy: Int, oz: Int, newShipCenter: Vector3d, originCenter: Vector3d, mapped: Map<ShipId, ShipId>) {
    val from = BlockPos(ox, oy, oz)
    val be = level.getBlockEntity(from)

    val to = BlockPos(
        newShipCenter.x + (ox.toDouble() - originCenter.x) + 0.5,
        newShipCenter.y + (oy.toDouble() - originCenter.y) + 0.5,
        newShipCenter.z + (oz.toDouble() - originCenter.z) + 0.5,
    )

    val tag = be?.let {
        val tag = it.saveWithFullMetadata()
        tag.putInt("x", to.x)
        tag.putInt("y", to.y)
        tag.putInt("z", to.z)

        if (tag.contains("ShiptraptionID")) {
            val prev = tag.getLong("ShiptraptionID")
            tag.putLong("ShiptraptionID", mapped.getOrDefault(prev, prev))
        }
        tag
    }
    level.getChunkAt(to).setBlockState(to, state, false)
    tag?.let { level.getBlockEntity(to)!!.load(tag) }
}

private fun updateBlock(level: Level, pos: BlockPos, newState: BlockState) {
    val flags = 11

    level.setBlocksDirty(pos, AIR, newState)
    level.sendBlockUpdated(pos, AIR, newState, flags)
    level.blockUpdated(pos, newState.block)

    if (!level.isClientSide && newState.hasAnalogOutputSignal()) {
        level.updateNeighbourForOutputSignal(pos, newState.block)
    }
    //This updates lighting for blocks in shipspace
    level.chunkSource.lightEngine.checkBlock(pos)
}

private fun createShip(level: ServerLevel, originShip: ServerShip, posInShipOffset: Vector3d, toPos: Vector3d): ServerShip {
    val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, 1.0, level.dimensionId)

    val posInShip = Vector3d(newShip.chunkClaim.xMiddle * 16, 128.5, newShip.chunkClaim.zMiddle * 16) + posInShipOffset

    newShip.isStatic = true
    level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
        toPos.toJomlVector3d(),
        originShip.transform.shipToWorldRotation,
        newScale = Vector3d(originShip.transform.shipToWorldScaling).avg()
    ))
    newShip.transformProvider = FixedPositionTransformProvider(toPos.toJomlVector3d(), posInShip.toJomlVector3d())

    return newShip
}

private fun checkLimits(traversedData: VSConstraintsTracker.TraversedData, player: ServerPlayer?): Boolean {
    if (player == null) {return true}

    if (VMConfig.SERVER.TOOLGUN.MAX_SHIPS_ALLOWED_TO_COPY > 0 && traversedData.traversedShipIds.size > VMConfig.SERVER.TOOLGUN.MAX_SHIPS_ALLOWED_TO_COPY) {return false}

    return true
}

fun copyShipWithConnections(level: ServerLevel, originShip: ServerShip, toRaycastResult: RaycastFunctions.RaycastResult, player: ServerPlayer? = null) {
    val traversed = VSConstraintsTracker.traverseGetConnectedShips(originShip.id)
    //why? phys entities exist
    traversed.traversedShipIds.removeAll(traversed.traversedShipIds.filter { !level.shipObjectWorld.loadedShips.contains(it) }.toSet())

    if (!checkLimits(traversed, player)) {return}

    //why? just in case
    val originShips = traversed.traversedShipIds.mapNotNull { level.shipObjectWorld.loadedShips.getById(it) }

    val objectAABB = AABBd(originShips[0].worldAABB)
    originShips.forEach {
        val b = it.worldAABB
        if (b.minX() < objectAABB.minX) { objectAABB.minX = b.minX() }
        if (b.maxX() > objectAABB.maxX) { objectAABB.maxX = b.maxX() }
        if (b.minY() < objectAABB.minY) { objectAABB.minY = b.minY() }
        if (b.maxY() > objectAABB.maxY) { objectAABB.maxY = b.maxY() }
        if (b.minZ() < objectAABB.minZ) { objectAABB.minZ = b.minZ() }
        if (b.maxZ() > objectAABB.maxZ) { objectAABB.maxZ = b.maxZ() }
    }

    val objectLogicalCenter = Vector3d(
        (objectAABB.maxX() - objectAABB.minX())/2,
        (objectAABB.maxY() - objectAABB.minY())/2,
        (objectAABB.maxZ() - objectAABB.minZ())/2
    )
    val minObjectHitboxPosition = Vector3d(objectAABB.minX, objectAABB.minY, objectAABB.minZ)


    val pos = toRaycastResult.worldCenteredHitPos!!

    val createdShips = originShips.map {
        val originOffset = Vector3d(it.transform.positionInShip) - Vector3d(it.chunkClaim.xMiddle * 16, 128.5, it.chunkClaim.zMiddle * 16)

        val posInWorld = Vector3d(it.transform.positionInWorld)

        val toPos = pos - (minObjectHitboxPosition + objectLogicalCenter) + posInWorld + (objectLogicalCenter * toRaycastResult.worldNormalDirection!!)

        createShip(level, originShip, originOffset, toPos)
    }

    val chunksToBeUpdated = mutableMapOf<ChunkPos, Pair<ChunkPos, ChunkPos>>()

    createdShips.forEach {ship ->
        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            chunksToBeUpdated[ChunkPos(chunkX, chunkZ)] = Pair(ChunkPos(chunkX, chunkZ), ChunkPos(chunkX, chunkZ))
        }
    }

    val chunkPairs = chunksToBeUpdated.values.toList()
    val chunkPoses = chunkPairs.flatMap { it.toList() }
    val chunkPosesJOML = chunkPoses.map { it.toJOML() }

    level.players().forEach { player ->
        PacketStopChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
    }

    var numCreatedShips = 0
    var totalNumShips = originShips.size

    val mapped = originShips.zip(createdShips).associate { Pair(it.first.id, it.second.id) }

    for (it in originShips.zip(createdShips)) {
        val originAABB = it.first.shipAABB!!
        // probably not really the center, but it doesn't actually matter
        val originCenter  = Vector3d(it.first .chunkClaim.xMiddle * 16 - 0.5, 128.5, it.first .chunkClaim.zMiddle * 16 - 0.5)
        val newShipCenter = Vector3d(it.second.chunkClaim.xMiddle * 16 - 0.5, 128.5, it.second.chunkClaim.zMiddle * 16 - 0.5)

        CreateShipPool.registerShipCreation(
                level, originAABB, {
            state, ox, oy, oz ->
            copyShipBlock(level, state, ox, oy, oz, newShipCenter, originCenter, mapped)
        }) {
            numCreatedShips++
            if (numCreatedShips < totalNumShips) { return@registerShipCreation }

            level.players().forEach { player ->
                PacketRestartChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
            }

            for ((original, created) in originShips.zip(createdShips)) {
                val tp = created.transformProvider
                if (tp !is FixedPositionTransformProvider) { continue }
                created.isStatic = false
                level.shipObjectWorld.teleportShip(created, ShipTeleportDataImpl(tp.positionInWorld, original.transform.shipToWorldRotation))
            }

            traversed.traversedMConstraintIds.forEach {
                val newConstraint = level.getManagedConstraint(it)?.copyMConstraint(level, mapped) ?: return@forEach
                level.makeManagedConstraint(newConstraint)
            }

            // why separate? we need it to give information about positionInShip to copyMConstraint somehow.
            // transformProvider carries that information
            for (created in createdShips) {
                created.transformProvider = null
            }
        }
    }
}

object CreateShipPool {
    val serverCreateShip = EventEmitterImpl<ServerCreateShip>()

    fun registerShipCreation(level: ServerLevel, boundsAABB: AABBic, copyFN: (state: BlockState, ox: Int, oy: Int, oz: Int) -> Unit, postFN: () -> Unit) {
        var oy = boundsAABB.minY() - 1
        var ox = boundsAABB.minX() - 1
        var oz = boundsAABB.minZ() - 1

        val toUpdate = mutableListOf<Pair<BlockPos, BlockState>>()
        var i = 0

        serverCreateShip.on {
            (start, maxTime), handler ->

            if (start + maxTime < getNow_ms()) {return@on}

            //TODO redo this to use chunks
            for (oy_ in oy until boundsAABB.maxY() + 1) {
            for (ox_ in ox until boundsAABB.maxX() + 1) {
            for (oz_ in oz until boundsAABB.maxZ() + 1) {
                val state = level.getBlockState(BlockPos(ox_, oy_, oz_))
                if (!state.isAir) {
                    copyFN(state, ox_, oy_, oz_)
                    toUpdate.add(Pair(BlockPos(ox_, oy_, oz_), state))
                }
//                if (getNow_ms() > start + maxTime) {
//                    ox = ox_
//                    oy = oy_
//                    oz = oz_ + 1
//                    return@on
//                }
            }
            oz = boundsAABB.minZ() - 1}
            ox = boundsAABB.minX() - 1; oz = boundsAABB.minZ() - 1}

            for (i_ in i until toUpdate.size) {
                updateBlock(level, toUpdate[i_].first, toUpdate[i_].second)
//                if (getNow_ms() > start + maxTime) {
//                    i = i_ + 1
//                    return@on
//                }
            }

            postFN()

            handler.unregister()
        }
    }

    data class ServerCreateShip(val start: Long, val maxTime: Long) {
        companion object : EventEmitter<ServerCreateShip> by serverCreateShip
    }

    init {
        TickEvent.SERVER_POST.register {
            serverCreateShip.emit(ServerCreateShip(getNow_ms(), 50L))
        }
    }
}