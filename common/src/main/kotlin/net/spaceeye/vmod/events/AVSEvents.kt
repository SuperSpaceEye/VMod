package net.spaceeye.vmod.events

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.game.ships.ShipData
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet

// Additional VS Events
object AVSEvents {

    val serverShipUnloadEvent = EventEmitterImpl<ServerShipUnloadEvent>()

    data class ServerShipUnloadEvent(val shipData: ShipDataCommon) {
        companion object : EventEmitter<ServerShipUnloadEvent> by serverShipUnloadEvent
    }

    val clientShipUnloadEvent = EventEmitterImpl<ClientShipUnloadEvent>()

    data class ClientShipUnloadEvent(val ship: Ship?) {
        companion object : EventEmitter<ClientShipUnloadEvent> by clientShipUnloadEvent
    }

    val serverShipRemoveEvent = EventEmitterImpl<ServerShipRemoveEvent>()

    data class ServerShipRemoveEvent(val ship: ShipData) {
        companion object : EventEmitter<ServerShipRemoveEvent> by serverShipRemoveEvent
    }

    //VS event is useless
    val splitShip = EventEmitterImpl<SplitShipEvent>()

    data class SplitShipEvent(
        val level: ServerLevel,
        val originalShip: ServerShip,
        val newShip: ServerShip,
        val centerBlock: BlockPos,
        val blocks: DenseBlockPosSet
    ) {
        companion object : EventEmitter<SplitShipEvent> by splitShip
    }
}