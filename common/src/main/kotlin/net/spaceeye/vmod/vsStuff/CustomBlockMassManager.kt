package net.spaceeye.vmod.vsStuff

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.PosMap
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

object CustomBlockMassManager {
    val dimToPosToMass = mutableMapOf<DimensionId, PosMap<Double>>()
    val shipToPosToMass = mutableMapOf<ShipId, PosMap<Double>>()

    fun getCustomMass(dimension: DimensionId, x: Int, y: Int, z: Int): Double? {
        return dimToPosToMass.getOrPut(dimension) { PosMap() }.getItemAt(x, y, z)
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, mass: Double): Boolean {
        val ship = level.getShipManagingPos(x shr 4, z shr 4) ?: return false
        val block = level.getBlockState(BlockPos(x, y, z))

        return setCustomMass(level, x, y, z, mass, block, ship)
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, mass: Double, state: BlockState, ship: ServerShip): Boolean {
        val (defaultMass, type) = BlockStateInfo.get(state) ?: return false
        val oldMass = getCustomMass(level.dimensionId, x, y, z) ?: defaultMass

        val dimensionId = level.dimensionId
        val shipObjectWorld = level.shipObjectWorld

        shipObjectWorld.onSetBlock(x, y, z, dimensionId, type, type, oldMass, mass)

        dimToPosToMass.getOrPut(level.dimensionId) { PosMap() }.setItemTo(mass, x, y, z)
        shipToPosToMass.getOrPut(ship.id) { PosMap() }.setItemTo(mass, x, y, z)
        return true
    }

    fun loadCustomMass(dimensionId: DimensionId, shipId: ShipId, x: Int, y: Int, z: Int, mass: Double) {
        dimToPosToMass.getOrPut(dimensionId) { PosMap() }.setItemTo(mass, x, y, z)
        shipToPosToMass.getOrPut(shipId) { PosMap() }.setItemTo(mass, x, y, z)
    }
}