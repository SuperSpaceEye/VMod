package net.spaceeye.vsource.blocks

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.vsource.WLOG
import net.spaceeye.vsource.blockentities.MessagerBlockEntity

class SimpleMessager(properties: Properties): BaseEntityBlock(properties) {
    init {
//        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false))
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (level !is ServerLevel) {return}
        if (level.hasNeighborSignal(pos)) {
            WLOG("A SIGNAL")
        } else {
            WLOG("NO SIGNAL")
        }

        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
    }

    private fun <T: BlockEntity?> makeTicker(): BlockEntityTicker<T> {
        return BlockEntityTicker {
            level, blockPos, blockState, blockEntity ->

        }
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level !is ServerLevel) { null } else { makeTicker() }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = MessagerBlockEntity(pos, state)
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) { super.onRemove(state, level, pos, newState, isMoving) }
}