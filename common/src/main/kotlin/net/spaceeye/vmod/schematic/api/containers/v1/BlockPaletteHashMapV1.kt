package net.spaceeye.vmod.schematic.api.containers.v1

import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.schematic.api.interfaces.IBlockStatePalette

open class BlockPaletteHashMapV1(bits: Int = 1): IBlockStatePalette {
    var statePaletteMap: CrudeIncrementalIntIdentityHashBiMap<BlockState> = CrudeIncrementalIntIdentityHashBiMap.create(1 shl bits)

    override fun toId(state: BlockState): Int {
        val id = statePaletteMap.getId(state)
        if (id != -1) {return id}
        return statePaletteMap.add(state)
    }

    override fun fromId(id: Int): BlockState? = statePaletteMap.byId(id)

    override fun getPaletteSize(): Int = statePaletteMap.size()

    override fun setPalette(newPalette: List<Pair<Int, BlockState>>) {
        statePaletteMap.clear()
        newPalette.forEach { (pos, state) -> statePaletteMap.addMapping(state, pos) }
    }
}