package net.spaceeye.vmod.compat.schem

import edn.stratodonut.trackwork.TrackBlocks
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

class TrackworkSchemCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    private var blocksSet: Set<Block>? = null

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, delayLoading: () -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (blocksSet == null) {
            blocksSet = setOf(
                TrackBlocks.LARGE_SUSPENSION_TRACK.get()
                ,TrackBlocks.MED_SUSPENSION_TRACK.get()
                ,TrackBlocks.SUSPENSION_TRACK.get()
                ,TrackBlocks.LARGE_PHYS_TRACK.get()
                ,TrackBlocks.MED_PHYS_TRACK.get()
                ,TrackBlocks.PHYS_TRACK.get()
                ,TrackBlocks.SIMPLE_WHEEL.get()
                ,TrackBlocks.SIMPLE_WHEEL_PART.get()
            )
        }
        if (!blocksSet!!.contains(state.block)) return
        tag.putBoolean("Assembled", false)
        tag.remove("trackBlockID")
    }
}