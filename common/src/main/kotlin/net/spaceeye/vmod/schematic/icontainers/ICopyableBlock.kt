package net.spaceeye.vmod.schematic.icontainers

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

interface ICopyableBlock {
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?, shipsBeingCopied: List<ServerShip>): CompoundTag?
    // calling delayLoading will delay loading of block entity until all ship blocks were created
    // return of this function is a callback that will be called after all ships were created and all delayed block entities were loaded
    fun onPaste(level: ServerLevel, pos: BlockPos, state: BlockState, oldToNewId: Map<Long, Long>, tag: CompoundTag?, delayLoading: () -> Unit): ((BlockEntity?) -> Unit)?
    fun onPasteNoTag(level: ServerLevel, pos: BlockPos, state: BlockState, oldToNewId: Map<Long, Long>) {}
}