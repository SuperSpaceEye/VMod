package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableForcesInducer
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.utils.ServerObjectsHolder
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple3
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ServerTickListener
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.function.Supplier

class WeightSynchronizer: ShipForcesInducer, ServerTickListener, ICopyableForcesInducer {
    var shipId = -1L
    var dimensionId = ""
    var lastDimensionId = ""

    var massPerBlock = 1.0
    var targetTotalMass = 1.0

    var syncMassPerBlock = true
    var syncWithConnectedStructure = false

    override fun applyForces(physShip: PhysShip) {}

    @JsonIgnore var level: ServerLevel? = null
    @JsonIgnore var updateWeights: Boolean = false

    override fun onServerTick() {
        if (lastDimensionId != dimensionId) {
            level = ServerObjectsHolder.getLevelById(dimensionId)
            lastDimensionId = dimensionId
        }
        if (!updateWeights) {return}
        val level = level ?: return
        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: return
        if (syncWithConnectedStructure) {
            // synchronize mass so that total mass of a structure is the same, but it distributed in a way that doesn't make physx constraints fail (or at least minimizes wonkiness)
            TODO()
        } else {
            updateMass(level, ship, false, syncMassPerBlock, massPerBlock, targetTotalMass)
        }
    }
    override fun onCopy(level: Supplier<ServerLevel>, shipOn: LoadedServerShip, shipsToBeSaved: List<ServerShip>, centerPositions: Map<ShipId, Vector3d>) {}
    override fun onPaste(
        level: Supplier<ServerLevel>,
        shipOn: LoadedServerShip,
        loadedShips: Map<Long, ServerShip>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>
    ) {
        shipId = shipOn.id
        dimensionId = shipOn.chunkClaimDimension
        updateWeights = true
    }

    companion object {
        init {
            PersistentEvents.onBlockStateChange.on { (level, pos, newState, isMoving), _ ->
                val ship = level.getShipObjectManagingPos(pos) ?: return@on
                val atch = ship.getAttachment<WeightSynchronizer>() ?: return@on
                atch.updateWeights = true
            }
        }

        @JvmStatic fun updateMass(
            level: ServerLevel,
            ship: ServerShip,
            resetMassToDefault: Boolean,
            syncWeightPerBlock: Boolean,
            weightPerBlock: Double,
            targetWeight: Double
        ) {
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
                val mass = if (resetMassToDefault) defaultMass else when (syncWeightPerBlock) {
                    true -> weightPerBlock
                    false -> defaultMass / defaultTotalMass * targetWeight
                }

                CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, mass, type, defaultMass, ship)
            }
        }

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<WeightSynchronizer>()
                ?: WeightSynchronizer().also {
                    it.shipId = ship.id
                    it.dimensionId = ship.chunkClaimDimension
                    ship.saveAttachment(it.javaClass, it)
                }
    }
}