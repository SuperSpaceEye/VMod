package net.spaceeye.vmod.vsStuff

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.PosMap
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

object CustomBlockMassManager {
    val dimToPosToMass = mutableMapOf<DimensionId, PosMap<Double>>()
    val shipToPosToMass = mutableMapOf<ShipId, PosMap<Double>>()

    fun removeCustomMass(dimensionId: DimensionId, x: Int, y: Int, z: Int) {
        dimToPosToMass.getOrPut(dimensionId) { PosMap() }.removeItemFromPos(x, y, z)
    }

    fun getCustomMass(dimension: DimensionId, x: Int, y: Int, z: Int): Double? {
        return dimToPosToMass.getOrPut(dimension) { PosMap() }.getItemAt(x, y, z)
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, mass: Double): Boolean {
        val ship = level.getShipManagingPos(x shr 4, z shr 4) ?: return false
        val block = level.getBlockState(BlockPos(x, y, z))
        val (defaultMass, type) = BlockStateInfo.get(block) ?: return false

        return setCustomMass(level, x, y, z, mass, type, defaultMass, ship)
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, mass: Double, type: BlockType, defaultMass: Double, ship: ServerShip): Boolean {
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