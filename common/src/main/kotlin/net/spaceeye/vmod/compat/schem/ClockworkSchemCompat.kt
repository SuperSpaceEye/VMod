package net.spaceeye.vmod.compat.schem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.vs.getCenterPos
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d

class ClockworkSchemCompat(): SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, delayLoading: (delay: Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (state.block != ClockworkBlocks.PHYS_BEARING.get()) {return}
        delayLoading(true) {
            val oldId = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
            val mapped = oldToNewId[oldId] ?: -1
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, mapped)
            val ship = level.shipObjectWorld.allShips.getById(mapped) ?: return@delayLoading tag

            val oldShiptraptionCenter = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER)!!
            tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER,
                oldShiptraptionCenter
                    .sub(getCenterPos(oldShiptraptionCenter.x.toInt(), oldShiptraptionCenter.z.toInt()).toJomlVector3d())
                    .add(getCenterPos(ship.transform.positionInShip.x().toInt(), ship.transform.positionInShip.z().toInt()).toJomlVector3d())
            )
            tag
        }
    }
}