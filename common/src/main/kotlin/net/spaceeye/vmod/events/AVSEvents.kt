package net.spaceeye.vmod.events

import net.spaceeye.vmod.utils.SafeEventEmitter
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.game.bodies.VsBodyDataCommon
import org.valkyrienskies.core.impl.networking.impl.PhysEntityCreateData

// Additional VS Events
object AVSEvents {
    val serverShipRemoveEvent = SafeEventEmitter<ServerShipRemoveEvent>()
    val clientShipUnloadEvent = SafeEventEmitter<ClientShipUnloadEvent>()

    val clientPhysEntityLoad = SafeEventEmitter<PhysEntityCreateData>()
    val clientPhysEntityUnload = SafeEventEmitter<Long>()

    val clientBodyLoad = SafeEventEmitter<VsBodyDataCommon>()
    val clientBodyUnload = SafeEventEmitter<Long>()

    data class ServerShipRemoveEvent(val ship: ServerShip)
    data class ClientShipUnloadEvent(val ship: Ship?)
}