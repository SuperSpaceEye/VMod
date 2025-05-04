package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple3
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ServerTickListener
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class WeightSynchronizer: ShipForcesInducer, ServerTickListener {
    @JsonIgnore var shipId: Long = -1
    @JsonIgnore var dimensionId: String = ""
    override fun applyForces(physShip: PhysShip) {}

    @JsonIgnore var level: ServerLevel? = null
    @JsonIgnore var lastDimensionId: String = ""
    @JsonIgnore var updateWeights: Boolean = false

    
    var targetWeight: Double = 1.0
    var syncWithConnectedStructure: Boolean = false

    private fun syncSelf(level: ServerLevel, ship: ServerShip) {
        val aabb = ship.shipAABB ?: return

        var defaultTotalMass = 0.0
        val blocks = mutableListOf<Tuple3<Double, BlockPos, BlockType>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue
            defaultTotalMass += mass
            blocks.add(Tuple.of(mass, bpos, type))
        } } }

        blocks.forEach { (defaultMass, pos, type) ->
            val mass = defaultMass / defaultTotalMass * targetWeight
            CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, mass, type, defaultMass, ship)
        }
    }

    override fun onServerTick() {
        if (lastDimensionId != dimensionId) {
            level = ServerLevelHolder.getLevelById(dimensionId)
            lastDimensionId = dimensionId
        }
        if (!updateWeights) {return}
        val level = level ?: return
        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: return
        if (syncWithConnectedStructure) {
            TODO()
        } else {
            syncSelf(level, ship)
        }
    }

    companion object {
        init {
            PersistentEvents.onBlockStateChange.on { (level, pos, newState, isMoving), _ ->
                val ship = level.getShipObjectManagingPos(pos) ?: return@on
                val atch = ship.getAttachment<WeightSynchronizer>() ?: return@on
                atch.updateWeights = true
            }
        }

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<WeightSynchronizer>()
                ?: WeightSynchronizer().also {
                    ship.setAttachment(it.javaClass, it)
                }
    }
}