package net.spaceeye.vmod

import dev.architectury.event.events.common.TickEvent
import net.spaceeye.vmod.utils.ServerClosable
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.locks.ReentrantLock

object VSMasslessShipsProcessor: ServerClosable() {
    val lock = ReentrantLock()

    val shipsToBeCreated = mutableSetOf<ShipId>()
    private var toRemove = mutableSetOf<ShipId>()

    init {
        TickEvent.SERVER_PRE.register {
            synchronized(lock) {
                toRemove.forEach { id ->
                    val ship = it.shipObjectWorld.allShips.getById(id) ?: return@forEach
                    it.shipObjectWorld.deleteShip(ship)
                }
                toRemove.clear()
            }
        }
    }

    fun process(shipId: ShipId): Boolean {
        if (shipsToBeCreated.contains(shipId)) { return false }
        if (!VMConfig.SERVER.AUTOREMOVE_MASSLESS_SHIPS) { return true }
        synchronized(lock) {
            toRemove.add(shipId)
        }
        return false
    }

    override fun close() {
        shipsToBeCreated.clear()
        toRemove.clear()
    }
}