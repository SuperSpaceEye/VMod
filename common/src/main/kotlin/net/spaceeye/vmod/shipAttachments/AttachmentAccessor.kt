package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.hooks.VSEvents

class AttachmentAccessor: ShipPhysicsListener {
    @JsonIgnore
    var forceInducers = listOf<ShipPhysicsListener>()

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        physShip as PhysShipImpl
        forceInducers = physShip.physicsListeners
    }

    companion object {
        init {
            VSEvents.shipLoadEvent.on { (ship) ->
                getOrCreate(ship)
            }
        }

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment(AttachmentAccessor::class.java)
                ?: AttachmentAccessor().also {
                    ship.setAttachment(it)
                }
    }
}