package net.spaceeye.vmod.compat.schem

import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock
import edn.stratodonut.trackwork.tracks.blocks.WheelBlock
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

class TrackworkSchemCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, delayLoading: (Boolean) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        val block = state.block
        if (block !is WheelBlock && block !is TrackBaseBlock<*>) {return}
        tag.putBoolean("Assembled", false)
        tag.remove("trackBlockID")
    }
}