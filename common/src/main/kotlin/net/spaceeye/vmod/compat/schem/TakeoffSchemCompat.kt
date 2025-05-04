package net.spaceeye.vmod.compat.schem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.tryMovePositionJ
import net.takeoff.TakeoffBlocks
import net.takeoff.blockentity.BearingBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class TakeoffSchemCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {
        if (   state.block != TakeoffBlocks.BEARING.get()
            && state.block != TakeoffBlocks.BEARING_TOP.get()
        ) { return }
        if (tag == null) { return }

        tag.putLong("VMOD_INJECT_otherId", level.getShipManagingPos(BlockPos.of(tag.getLong("otherPos")))?.id ?: return)
    }

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((BlockEntity?) -> Unit) -> Unit) {
        if (   state.block != TakeoffBlocks.BEARING.get()
            && state.block != TakeoffBlocks.BEARING_TOP.get()
        ) { return }
        if (!tag.contains("VMOD_INJECT_otherId")) {return}
        if (!tag.getBoolean("isBase")) {return}

        val newOtherShip = level.shipObjectWorld.allShips.getById(oldToNewId[tag.getLong("VMOD_INJECT_otherId")]!!)!!
        val newPos = tryMovePositionJ(Vector3d(BlockPos.of(tag.getLong("otherPos"))), oldToNewId[tag.getLong("VMOD_INJECT_otherId")]!!, centerPositions)!!

        tag.putLong("otherPos", newPos.toBlockPos().asLong())

        afterPasteCallbackSetter {
            it as BearingBlockEntity
            it.createConstraints(newOtherShip)
        }
    }
}