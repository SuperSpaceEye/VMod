package net.spaceeye.vmod.utils

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Clearable
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
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

fun copyShipWithConnections(level: ServerLevel, originShip: ServerShip, toRaycastResult: RaycastFunctions.RaycastResult) {
    // making so that ship will be at logical center after teleportation
    val b = originShip.worldAABB
    val logicalCenter = Vector3d(
        (b.maxX() - b.minX())/2,
        (b.maxY() - b.minY())/2,
        (b.maxZ() - b.minZ())/2
    )
    val physCenter = Vector3d(originShip.transform.positionInWorld)
    val minHitboxPos = Vector3d(b.minX(), b.minY(), b.minZ())

    val offset = physCenter - (minHitboxPos + logicalCenter)
    // shifting position so that
    val secondOffset = logicalCenter * toRaycastResult.worldNormalDirection!!

    val newPos = toRaycastResult.worldCenteredHitPos!! + offset + secondOffset
    val newShip = level.shipObjectWorld.createNewShipAtBlock(newPos.toJomlVector3i(), false, Vector3d(originShip.transform.shipToWorldScaling).avg(), level.dimensionId)
    newShip.isStatic = true
    level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
        newPos.toJomlVector3d()
    ))


    val originAABB = originShip.shipAABB!!
    val originCenter = Vector3d(originShip.transform.positionInShip)

    val newShipCenter = Vector3d(newShip.transform.positionInShip)

    CreateShipPool.serverCreateShip.on {
        it, handler ->
    for (oy in originAABB.minY() until  originAABB.maxY()) {
    for (ox in originAABB.minX() until  originAABB.maxX()) {
    for (oz in originAABB.minZ() until  originAABB.maxZ()) {
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