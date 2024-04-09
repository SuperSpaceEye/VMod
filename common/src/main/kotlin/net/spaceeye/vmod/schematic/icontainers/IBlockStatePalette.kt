package net.spaceeye.vmod.schematic.icontainers

import net.minecraft.world.level.block.state.BlockState

interface IBlockStatePalette {
    fun toId(state: BlockState): Int
    fun fromId(id: Int): BlockState?

    fun getPaletteSize(): Int
    val paletteVersion: Int

    fun setPalette(newPalette: List<Pair<Int, BlockState>>)
}