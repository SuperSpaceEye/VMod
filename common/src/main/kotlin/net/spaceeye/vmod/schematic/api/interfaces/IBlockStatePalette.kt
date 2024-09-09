package net.spaceeye.vmod.schematic.api.interfaces

import net.minecraft.world.level.block.state.BlockState

interface IBlockStatePalette {
    fun toId(state: BlockState): Int
    fun fromId(id: Int): BlockState?

    fun getPaletteSize(): Int

    fun setPalette(newPalette: List<Pair<Int, BlockState>>)
}