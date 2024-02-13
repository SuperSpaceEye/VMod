package net.spaceeye.vsource.events

import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.game.ships.ShipData
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl

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
}