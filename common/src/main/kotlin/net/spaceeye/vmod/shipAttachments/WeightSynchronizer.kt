package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.utils.ServerObjectsHolder
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ServerTickListener
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.assembly.ICopyableAttachment
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

class WeightSynchronizer: ShipPhysicsListener, ServerTickListener, ICopyableAttachment {
    var shipId = -1L
    var dimensionId = ""
    var lastDimensionId = ""

    var massPerBlock = 1.0
    var targetTotalMass = 1.0

    var syncMassPerBlock = true
    var syncWithConnectedStructure = false

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {}

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
    override fun onCopy(level: Supplier<ServerLevel>, shipOn: LoadedServerShip, shipsToBeSaved: List<ServerShip>, centerPositions: Map<ShipId, Vector3dc>) {}
    override fun onPaste(
        level: Supplier<ServerLevel>,
        shipOn: LoadedServerShip,
        loadedShips: Map<Long, ServerShip>,
        centerPositions: Map<ShipId, Pair<Vector3dc, Vector3dc>>
    ) {
        shipId = shipOn.id
        dimensionId = shipOn.chunkClaimDimension
        updateWeights = true
    }

    companion object {
        init {
            PersistentEvents.onBlockStateChange.on { (level, pos, newState, isMoving), _ ->
                val ship = level.getShipObjectManagingPos(pos) ?: return@on
                val atch = ship.getAttachment(WeightSynchronizer::class.java) ?: return@on
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
            val (_, air) = BlockStateInfo.get(Blocks.AIR.defaultBlockState())!!
            val aabb = ship.shipAABB ?: return
            val wasStatic = ship.isStatic
            ship.isStatic = true

            CustomBlockMassManager.zeroMass(level, ship)

            var minW =  Double.MAX_VALUE
            var maxW = -Double.MAX_VALUE
            var defaultTotalMass = 0.0
            val mbpos = BlockPos.MutableBlockPos(0, 0, 0)

            //TODO not efficient, but do i care?
            if (!syncWeightPerBlock && !resetMassToDefault) {
                for (x in aabb.minX()-1..aabb.maxX()+1) {
                for (z in aabb.minZ()-1..aabb.maxZ()+1) {
                for (y in aabb.minY()-1..aabb.maxY()+1) {
                    mbpos.set(x, y, z)
                    val state = level.getBlockState(mbpos)
                    if (state.isAir) {continue}
                    val (mass, _) = BlockStateInfo.get(state) ?: continue

                    minW = min(minW, mass)
                    maxW = max(maxW, mass)

                    defaultTotalMass += mass
                } } }
            }

            for (x in aabb.minX() until aabb.maxX()) {
            for (y in aabb.minY() until aabb.maxY()) {
            for (z in aabb.minZ() until aabb.maxZ()) {
                mbpos.set(x, y, z)
                val state = level.getBlockState(mbpos)
                if (state.isAir) {continue}
                val (dmass, type) = BlockStateInfo.get(state) ?: continue

                val mass = if (syncWeightPerBlock && !resetMassToDefault) {
                    weightPerBlock
                } else {
                    if (resetMassToDefault) {
                        dmass
                    } else {
                        dmass / defaultTotalMass * targetWeight
                    }
                }

                CustomBlockMassManager.setCustomMass(level, x, y, z, 0.0, mass, air, type, ship)
            } } }

            ship.isStatic = wasStatic
        }

        @Deprecated("ADDING IT TO SHIP WILL MAKE IT ACTIVATE")
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment(WeightSynchronizer::class.java)
                ?: WeightSynchronizer().also {
                    it.shipId = ship.id
                    it.dimensionId = ship.chunkClaimDimension
                    ship.setAttachment(it)
                }
    }
}