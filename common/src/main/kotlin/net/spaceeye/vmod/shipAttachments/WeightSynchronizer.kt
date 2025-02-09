package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerTickListener
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.common.shipObjectWorld

class WeightSynchronizer: ShipForcesInducer, ServerTickListener {
    @JsonIgnore var shipId: Long = -1
    @JsonIgnore var dimensionId: String = ""
    override fun applyForces(physShip: PhysShip) {
        shipId = physShip.id
        dimensionId = physShip.chunkClaimDimension
    }

    @JsonIgnore var level: ServerLevel? = null
    @JsonIgnore var lastDimensionId: String = ""

    override fun onServerTick() {
        if (lastDimensionId != dimensionId) {
            level = ServerLevelHolder.getLevelById(dimensionId)
            lastDimensionId = dimensionId
        }
        val level = level ?: return
        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: return


    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<WeightSynchronizer>()
                ?: WeightSynchronizer().also {
                    ship.setAttachment(it)
                }
    }
}