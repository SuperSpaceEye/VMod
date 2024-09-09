package net.spaceeye.vmod.schematic.api.interfaces.v1

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.api.containers.v1.BlockItem
import net.spaceeye.vmod.schematic.api.interfaces.IBlockStatePalette
import net.spaceeye.vmod.schematic.api.containers.v1.ChunkyBlockData
import org.valkyrienskies.core.api.ships.properties.ShipId

interface IShipSchematicDataV1 {
    var blockPalette: IBlockStatePalette
    var blockData: MutableMap<ShipId, ChunkyBlockData<BlockItem>>

    /**
     * Index of the item is the extraDataId in BlockItem
     */
    var flatTagData: MutableList<CompoundTag>

    /**
     * TODO explanation
     */
    var extraData: MutableList<Pair<String, Serializable>>
}