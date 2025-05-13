package net.spaceeye.vmod.compat.schem

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.mixin.SmartBlockEntityAccessor
import net.spaceeye.vmod.utils.JVector3d
import org.valkyrienskies.core.api.ships.ServerShip

class CreateBlockOverride(): SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((BlockEntity?) -> Unit) -> Unit) {
        delayLoading(true) {
            val be = level.getChunkAt(pos).getBlockEntity(pos)
            if (be !is SmartBlockEntity) {return@delayLoading null}
            (be as SmartBlockEntityAccessor).`vmod$setFirstNbtRead`(true)
            null
        }
    }
}