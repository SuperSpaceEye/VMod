package net.spaceeye.vmod.schematic.containers

import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.schematic.icontainers.IBlockStatePalette

class BlockPaletteHashMapV1(bits: Int = 1): IBlockStatePalette {
    var statePaletteMap: CrudeIncrementalIntIdentityHashBiMap<BlockState> = CrudeIncrementalIntIdentityHashBiMap.create(1 shl bits)

    override fun toId(state: BlockState): Int {
        val id = statePaletteMap.getId(state)
        if (id != -1) {return id}
        return statePaletteMap.add(state)
    }

    override fun fromId(id: Int): BlockState? = statePaletteMap.byId(id)

    override fun writeToFile() {
        TODO("Not yet implemented")
    }

    override fun writeFromFile() {
        TODO("Not yet implemented")
    }

    override fun getPaletteSize(): Int = statePaletteMap.size()
    override val paletteVersion: Int = 1
}