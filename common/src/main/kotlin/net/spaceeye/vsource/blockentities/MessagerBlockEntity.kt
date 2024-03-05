package net.spaceeye.vsource.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vsource.VSBlockEntities

class MessagerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(VSBlockEntities.SIMPLE_MESSAGER.get(), pos, state) {
}