package net.spaceeye.vmod.blocks

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
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
import net.spaceeye.vmod.network.Signal
import kotlin.math.roundToInt

class SimpleMessager(properties: Properties): BaseEntityBlock(properties) {
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

    private fun transmitMode(blockEntity: SimpleMessagerBlockEntity, level: ServerLevel, blockPos: BlockPos) {
        val msg = blockEntity.msg
        if (!level.hasNeighborSignal(blockPos)) {
            if (msg is Signal && msg.percentage != 0.0) {
                msg.percentage = 0.0
                MessagingNetwork.notify(blockEntity.channel, msg)
            }
            return
        }
        if (msg is Signal) {
            val signal = level.getBestNeighborSignal(blockPos)
            msg.percentage = signal.toDouble() / 15.0
        }
        MessagingNetwork.notify(blockEntity.channel, msg)
    }

    private fun receiveMode(blockEntity: SimpleMessagerBlockEntity, level: ServerLevel, blockPos: BlockPos) {
        val msg = blockEntity.msg
        if (msg is Signal) {
            val signal = 1.0 - msg.percentage

            val redstoneSignal = (signal * 15.0).roundToInt()
            if (blockEntity.lastSignal == redstoneSignal) {return}
            blockEntity.lastSignal = redstoneSignal

            level.scheduleTick(blockPos, this, 2)
            level.updateNeighborsAt(blockPos, this)
            level.updateNeighbourForOutputSignal(blockPos, this)
        }
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity !is SimpleMessagerBlockEntity) {return super.getDirectSignal(state, level, pos, direction)}
        if (blockEntity.transmit) {return 0}
        val msg = blockEntity.msg
        if (msg is Signal) {
            val signal = 1.0 - msg.percentage
            val redstoneSignal = (signal * 15.0).roundToInt()
            return redstoneSignal
        }

        return super.getDirectSignal(state, level, pos, direction)
    }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
    }

    private fun <T: BlockEntity?> makeTicker(): BlockEntityTicker<T> {
        return BlockEntityTicker {
            level, blockPos, blockState, blockEntity ->
            if (level !is ServerLevel) {return@BlockEntityTicker}
            if (blockEntity !is SimpleMessagerBlockEntity) {return@BlockEntityTicker}
            if (blockEntity.transmit) {
                transmitMode(blockEntity, level, blockPos)
            } else {
                receiveMode(blockEntity, level, blockPos)
            }
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