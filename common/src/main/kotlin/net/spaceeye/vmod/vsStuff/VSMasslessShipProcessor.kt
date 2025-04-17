package net.spaceeye.vmod.vsStuff

import dev.architectury.event.events.common.TickEvent
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.utils.ServerClosable
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.locks.ReentrantLock

object VSMasslessShipProcessor: ServerClosable() {
    val lock = ReentrantLock()

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
        if (!VMConfig.SERVER.AUTOREMOVE_MASSLESS_SHIPS) { return true }
        synchronized(lock) {
            toRemove.add(shipId)
        }
        return false
    }

    override fun close() {
        toRemove.clear()
    }
}