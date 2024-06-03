package net.spaceeye.vmod.compat.schem

import edn.stratodonut.trackwork.TrackBlocks
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

class TrackworkSchemCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, delayLoading: () -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (  state.block != TrackBlocks.LARGE_SUSPENSION_TRACK.get()
           && state.block != TrackBlocks.MED_SUSPENSION_TRACK.get()
           && state.block != TrackBlocks.SUSPENSION_TRACK.get()
           && state.block != TrackBlocks.LARGE_PHYS_TRACK.get()
           && state.block != TrackBlocks.MED_PHYS_TRACK.get()
           && state.block != TrackBlocks.PHYS_TRACK.get()
        ) {return}
        tag.putBoolean("Assembled", false)
        tag.remove("trackBlockID")
    }
}