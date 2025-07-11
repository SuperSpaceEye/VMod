package net.spaceeye.vmod.events

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.SafeEventEmitter
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.game.phys_entities.PhysicsEntityClient
import org.valkyrienskies.core.impl.game.ships.ShipData
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet

// Additional VS Events
object AVSEvents {
    val serverShipRemoveEvent = SafeEventEmitter<ServerShipRemoveEvent>()
    val clientShipUnloadEvent = SafeEventEmitter<ClientShipUnloadEvent>()
    val splitShip = SafeEventEmitter<SplitShipEvent>() //VS event is useless

    val clientPhysEntityLoad = SafeEventEmitter<ClientPhysEntityLoad>()
    val clientPhysEntityUnload = SafeEventEmitter<Long>()

    data class ServerShipRemoveEvent(val ship: ShipData)
    data class ClientShipUnloadEvent(val ship: Ship?)

    data class SplitShipEvent(
        val level: ServerLevel,
        val originalShip: ServerShip,
        val newShip: ServerShip,
        val centerBlock: BlockPos,
        val blocks: DenseBlockPosSet
    )

    data class ClientPhysEntityLoad(val data: PhysicsEntityClient)
}