package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.hooks.VSEvents

class AttachmentAccessor: ShipForcesInducer {
    @JsonIgnore
    var forceInducers = listOf<ShipForcesInducer>()

    override fun applyForces(physShip: PhysShip) {
        physShip as PhysShipImpl
        forceInducers = physShip.forceInducers
    }

    companion object {
        init {
            VSEvents.shipLoadEvent.on { (ship) ->
                getOrCreate(ship)
            }
        }

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<AttachmentAccessor>()
                ?: AttachmentAccessor().also {
                    ship.setAttachment(it.javaClass, it)
                }
    }
}