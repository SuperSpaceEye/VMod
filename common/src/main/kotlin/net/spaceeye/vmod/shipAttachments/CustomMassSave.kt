package net.spaceeye.vmod.shipAttachments

import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.joml.Vector3i
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer

class CustomMassSave: ShipForcesInducer {
    var shipId = -1L
    var dimensionId: String = ""
    var massSave: List<Pair<Vector3i, Double>>?
        get() {
            val data = CustomBlockMassManager.shipToPosToMass[shipId] ?: return null
            return data.asList()
        }
        set(value) {
            value?.forEach { (pos, mass) -> CustomBlockMassManager.loadCustomMass(dimensionId, shipId, pos.x, pos.y, pos.z, mass) }
        }
    
    override fun applyForces(physShip: PhysShip) {}

    companion object {
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<CustomMassSave>()
                ?: CustomMassSave().also {
                    it.shipId = ship.id
                    it.dimensionId = ship.chunkClaimDimension
                    ship.setAttachment(it.javaClass, it)
                }
    }
}