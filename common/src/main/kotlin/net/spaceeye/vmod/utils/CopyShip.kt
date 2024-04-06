package net.spaceeye.vmod.utils

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Clearable
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.constraintsManaging.getManagedConstraint
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.transformProviders.FixedPositionTransformProvider
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

private inline fun copyShipBlock(level: ServerLevel, state: BlockState, ox: Int, oy: Int, oz: Int, newShipCenter: Vector3d, originCenter: Vector3d, mapped: Map<ShipId, ShipId>) {
    val from = BlockPos(ox, oy, oz)
    val be = level.getBlockEntity(from)

    val to = BlockPos(
            (newShipCenter.x + (ox.toDouble() - originCenter.x) + 0.5).toInt(),
            (newShipCenter.y + (oy.toDouble() - originCenter.y) + 0.5).toInt(),
            (newShipCenter.z + (oz.toDouble() - originCenter.z) + 0.5).toInt(),
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

        // so that it won't drop its contents
        if (it is Clearable) {
            it.clearContent()
        }

        // so loot containers dont drop its content
        if (it is RandomizableContainerBlockEntity) {
            it.setLootTable(null, 0)
        }

        tag
    }
    level.getChunkAt(to).setBlockState(to, state, false)
    tag?.let { level.getBlockEntity(to)!!.load(tag) }
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

fun copyShipWithConnections(level: ServerLevel, originShip: ServerShip, toRaycastResult: RaycastFunctions.RaycastResult) {
    val traversed = VSConstraintsKeeper.traverseGetConnectedShips(originShip.id)

    val originShips = traversed.traversedShipIds.map { level.shipObjectWorld.loadedShips.getById(it)!! }

    val objectAABB = AABBd()
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
            if (numCreatedShips < totalNumShips) {return@registerShipCreation}

            for ((original, created) in originShips.zip(createdShips)) {
                val tp = created.transformProvider
                if (tp !is FixedPositionTransformProvider) { continue }
//                created.isStatic = false
                level.shipObjectWorld.teleportShip(created, ShipTeleportDataImpl(tp.positionInWorld, original.transform.shipToWorldRotation))
            }

            traversed.traversedMConstraintIds.forEach {
                val newConstraint = level.getManagedConstraint(ManagedConstraintId(it))?.copyMConstraint(level, mapped) ?: return@forEach
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
    private val serverCreateShip = EventEmitterImpl<ServerCreateShip>()

    fun registerShipCreation(level: ServerLevel, boundsAABB: AABBic, copyFN: (state: BlockState, ox: Int, oy: Int, oz: Int) -> Unit, postFN: () -> Unit) {
        var oy = boundsAABB.minY()
        var ox = boundsAABB.minX()
        var oz = boundsAABB.minZ()

        serverCreateShip.on {
            (start, maxTime), handler ->

            //TODO redo this to use chunks
            for (oy_ in oy until boundsAABB.maxY()) {
            for (ox_ in ox until boundsAABB.maxX()) {
            for (oz_ in oz until boundsAABB.maxZ()) {
                val state = level.getBlockState(BlockPos(ox_, oy_, oz_))
                if (!state.isAir) { copyFN(state, ox_, oy_, oz_) }
                if (getNow_ms() > start + maxTime) {
                    ox = ox_
                    oy = oy_
                    oz = oz_ + 1
                    return@on
                }
            }
            oz = boundsAABB.minZ()}
            ox = boundsAABB.minX(); oz = boundsAABB.minZ()}
            postFN()

            handler.unregister()
        }
    }

    data class ServerCreateShip(val start: Long, val maxTime: Long) {
        companion object : EventEmitter<ServerCreateShip> by serverCreateShip
    }

    init {
        TickEvent.SERVER_PRE.register {
            serverCreateShip.emit(ServerCreateShip(getNow_ms(), 50L))
        }
    }
}