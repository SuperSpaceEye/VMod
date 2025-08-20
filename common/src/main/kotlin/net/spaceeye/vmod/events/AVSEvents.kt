package net.spaceeye.vmod.events

import net.spaceeye.vmod.utils.SafeEventEmitter
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.game.phys_entities.PhysicsEntityClient
import org.valkyrienskies.core.impl.game.ships.ShipData

// Additional VS Events
object AVSEvents {
    val serverShipRemoveEvent = SafeEventEmitter<ServerShipRemoveEvent>()
    val clientShipUnloadEvent = SafeEventEmitter<ClientShipUnloadEvent>()

    val clientPhysEntityLoad = SafeEventEmitter<ClientPhysEntityLoad>()
    val clientPhysEntityUnload = SafeEventEmitter<Long>()

    data class ServerShipRemoveEvent(val ship: ShipData)
    data class ClientShipUnloadEvent(val ship: Ship?)

    data class ClientPhysEntityLoad(val data: PhysicsEntityClient)
}