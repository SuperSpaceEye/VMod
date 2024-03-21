package net.spaceeye.vmod.blocks

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vmod.blockentities.SimpleMessagerBlockEntity
import net.spaceeye.vmod.gui.SimpleMessagerGUI
import net.spaceeye.vmod.network.MessagingNetwork

class SimpleMessager(properties: Properties): BaseEntityBlock(properties) {
    init {
//        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false))
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (player is ServerPlayer) { return InteractionResult.CONSUME }
        if (level !is ClientLevel) { return InteractionResult.CONSUME }

        SimpleMessagerGUI.tryOpen(level, pos)

        return InteractionResult.SUCCESS
    }

    private fun <T: BlockEntity?> makeTicker(): BlockEntityTicker<T> {
        return BlockEntityTicker {
            level, blockPos, blockState, blockEntity ->
            if (blockEntity !is SimpleMessagerBlockEntity) {return@BlockEntityTicker}
            if (!level.hasNeighborSignal(blockPos)) {return@BlockEntityTicker}
            MessagingNetwork.notify(blockEntity.channel, blockEntity.msg)
        }
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level !is ServerLevel) { null } else { makeTicker() }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SimpleMessagerBlockEntity(pos, state)
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) { super.onRemove(state, level, pos, newState, isMoving) }
}