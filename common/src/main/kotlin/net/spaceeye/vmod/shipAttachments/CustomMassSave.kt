package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableForcesInducer
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.joml.Vector3i
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.hooks.VSEvents
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.roundToInt

class CustomMassSave(): ShipForcesInducer, ICopyableForcesInducer {
    var wasCopied: Boolean = false
    @JsonIgnore private var tempMassData: List<Pair<Vector3i, Double>>? = null
    @JsonIgnore var lock = ReentrantLock()

    var shipId: Long = -1L
    var dimensionId: String = ""
    var massSave: List<Pair<Vector3i, Double>>?
        get() {
            val data = CustomBlockMassManager.shipToPosToMass[shipId] ?: return null
            return data.asList()
        }
        set(value) {
            if (wasCopied) {
                tempMassData = value
                return
            }
            value?.forEach { (pos, mass) -> CustomBlockMassManager.loadCustomMass(dimensionId, shipId, pos.x, pos.y, pos.z, mass) }
        }

    override fun onCopy(level: ServerLevel, shipOn: LoadedServerShip, shipsToBeSaved: List<ServerShip>, centerPositions: Map<ShipId, Vector3d>) {
        lock.lock()
        wasCopied = true
    }

    override fun onAfterCopy(level: ServerLevel, shipOn: LoadedServerShip, shipsToBeSaved: List<ServerShip>, centerPositions: Map<ShipId, Vector3d>) {
        lock.unlock()
    }

    override fun onPaste(
        level: ServerLevel,
        shipOn: LoadedServerShip,
        loadedShips: Map<Long, ServerShip>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>
    ) {
        wasCopied = false

        if (tempMassData == null) {return}

        val newShip = loadedShips[shipId]!!
        val (oldCenter, newCenter) = centerPositions[shipId]!!

        dimensionId = newShip.chunkClaimDimension
        shipId = newShip.id

        tempMassData!!.forEach { (pos, mass) ->
            val pos = Vector3d(pos)
                .add(0.01, 0.01, 0.01)
                .sub(oldCenter)
                .add(newCenter)
                .let { Vector3i(it.x.roundToInt(), it.y.roundToInt(), it.z.roundToInt()) }
            CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, mass, )
        }
        tempMassData = null
    }
    
    override fun applyForces(physShip: PhysShip) {
        // not the most elegant solution but it'll work, probably
        if (!lock.tryLock()) {return}
        wasCopied = false
        lock.unlock()
    }

    companion object {
        init {
            VSEvents.shipLoadEvent.on { (ship) ->
                getOrCreate(ship)
            }
        }

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<CustomMassSave>()
                ?: CustomMassSave().also {
                    it.shipId = ship.id
                    it.dimensionId = ship.chunkClaimDimension
                    ship.saveAttachment(it.javaClass, it)
                }
    }
}