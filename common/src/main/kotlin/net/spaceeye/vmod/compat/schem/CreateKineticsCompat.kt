package net.spaceeye.vmod.compat.schem

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.JVector3d
import org.valkyrienskies.core.api.ships.ServerShip

class CreateKineticsCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((BlockEntity?) -> Unit) -> Unit) {
        if (state.block !is IBE<*>) {return}
        delayLoading(false) { tag ->
            val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: return@delayLoading tag
            be.warnOfMovement()
            tag
        }
    }
}