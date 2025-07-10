package net.spaceeye.vmod.events

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.SafeEventEmitter
import org.joml.Vector3ic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.networking.impl.PhysEntityCreateData

// Additional VS Events
object AVSEvents {
    val serverShipRemoveEvent = SafeEventEmitter<ServerShipRemoveEvent>()
    val blocksWereMovedEvent = SafeEventEmitter<BlocksMovedEvent>()

    val clientPhysEntityLoad = SafeEventEmitter<PhysEntityCreateData>()
    val clientPhysEntityUnload = SafeEventEmitter<Long>()

    data class ServerShipRemoveEvent(val ship: ServerShip)

    data class BlocksMovedEvent(
        val level: ServerLevel,
        val originalShip: ServerShip?,
        val newShip: ServerShip,
        val oldCenter: Vector3ic,
        val newCenter: Vector3ic,
        val blocks: List<BlockPos>
    )
}