package net.spaceeye.vmod.schematic.api.interfaces

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

interface ICopyableBlock {
    /**
     * Should be called on copy
     * @return If returns tag, then copy fn should save that tag. If returns null, then copy fn should get save tag from block entity itself.
     */
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?, shipsBeingCopied: List<ServerShip>): CompoundTag?
    /**
     * Should be called after all ships are created and all blocks are placed, but block entities are not loaded.
     * @param finalCallbackAdder Allows adding a callback that will be called after all ships were created, all blocks were placed, and all block entities were loaded
     */
    fun onPaste(level: ServerLevel, pos: BlockPos, state: BlockState, oldShipIdToNewId: Map<Long, Long>, tag: CompoundTag?, finalCallbackAdder: (callback: (BlockEntity?) -> Unit) -> Unit)

    /**
     * Should be called for simple blocks
     */
    fun onPasteNoTag(level: ServerLevel, pos: BlockPos, state: BlockState, oldShipIdToNewId: Map<Long, Long>) {}
}