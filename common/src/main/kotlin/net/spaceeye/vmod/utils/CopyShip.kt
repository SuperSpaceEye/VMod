package net.spaceeye.vmod.utils

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Clearable
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

//fun ServerShipWorldCore.teleportShipWithHitboxCenter(ship: ServerShip, data: ShipTeleportData) {
//    val b = ship.worldAABB
//    val logicalCenter = Vector3d(
//        (b.maxX() - b.minX())/2,
//        (b.maxY() - b.minY())/2,
//        (b.maxZ() - b.minZ())/2
//    )
//    val physCenter = Vector3d(ship.transform.positionInWorld)
//    val minHitboxPos = Vector3d(b.minX(), b.minY(), b.minZ())
//
//    val offset = physCenter - (minHitboxPos + logicalCenter)
//
//    data as ShipTeleportDataImpl
//    this.teleportShip(ship, data.copy(
//        (Vector3d(data.newPos) + offset).toJomlVector3d()
//
//    ))
//}

private fun copyShipBlocks(level: ServerLevel, originShip: ServerShip, newShip: ServerShip) {
    val originAABB = originShip.shipAABB!!
    val originCenter = Vector3d(originShip.transform.positionInShip)

    val newShipCenter = Vector3d(newShip.transform.positionInShip)

    for (oy in originAABB.minY() until originAABB.maxY()) {
    for (ox in originAABB.minX() until originAABB.maxX()) {
    for (oz in originAABB.minZ() until originAABB.maxZ()) {
        val from = BlockPos(ox, oy, oz)
        val state = level.getBlockState(from)
        if (state.isAir) {continue}
        val be = level.getBlockEntity(from)

        val to = BlockPos(
            newShipCenter.x + (ox - originCenter.x),
            newShipCenter.y + (oy - originCenter.y),
            newShipCenter.z + (oz - originCenter.z),
        )

        val tag = be?.let {
            val tag = it.saveWithFullMetadata()
            tag.putInt("x", to.x)
            tag.putInt("y", to.y)
            tag.putInt("z", to.z)

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
    } } }
}

fun createShip(level: ServerLevel, originShip: ServerShip, center: Vector3d, toCenter: Vector3d): ServerShip {
    val newPos = Vector3d(originShip.transform.positionInWorld) - center + toCenter

    val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, 1.0, level.dimensionId)
    newShip.isStatic = true
    level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
        newPos.toJomlVector3d(),
        originShip.transform.shipToWorldRotation,
        newScale = Vector3d(originShip.transform.shipToWorldScaling).avg()
    ))

    return newShip
}

//fun copyShipWithConnections(level: ServerLevel, originShip: ServerShip, toRaycastResult: RaycastFunctions.RaycastResult) {
//    // making so that ship will be at logical center after teleportation
//    val b = originShip.worldAABB
//    val logicalCenter = Vector3d(
//        (b.maxX() - b.minX())/2,
//        (b.maxY() - b.minY())/2,
//        (b.maxZ() - b.minZ())/2
//    )
//    val physCenter = Vector3d(originShip.transform.positionInWorld)
//    val minHitboxPos = Vector3d(b.minX(), b.minY(), b.minZ())
//
//    val offset = physCenter - (minHitboxPos + logicalCenter)
//    val secondOffset = logicalCenter * toRaycastResult.worldNormalDirection!!
//
//    val newPos = toRaycastResult.worldCenteredHitPos!! + offset + secondOffset
//    val newShip = level.shipObjectWorld.createNewShipAtBlock(newPos.toJomlVector3i(), false, Vector3d(originShip.transform.shipToWorldScaling).avg(), level.dimensionId)
//    newShip.isStatic = true
//    level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
//        newPos.toJomlVector3d()
//    ))
//
//    CreateShipPool.serverCreateShip.on {
//        it, handler ->
//        copyShipBlocks(level, originShip, newShip)
//        handler.unregister()
//    }
//}

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

    val toCenter = toRaycastResult.worldCenteredHitPos!! + objectLogicalCenter * toRaycastResult.worldNormalDirection!!

    val createdShips = originShips.map { createShip(level, it, objectLogicalCenter + minObjectHitboxPosition, toCenter) }

    CreateShipPool.serverCreateShip.on {
    _, handler ->
    originShips.zip(createdShips).forEach {
        copyShipBlocks(level, it.first, it.second)
    }

//    traversed.traversedMConstraintIds.forEach {
//
//    }
//
//    createdShips.forEach { it.isStatic = false }

    handler.unregister()
    }
}

object CreateShipPool {
    val serverCreateShip = EventEmitterImpl<ServerCreateShip>()

    class ServerCreateShip() {
        companion object : EventEmitter<ServerCreateShip> by serverCreateShip
    }

    init {
        TickEvent.SERVER_PRE.register {
            serverCreateShip.emit(ServerCreateShip())
        }
    }
}