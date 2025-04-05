package net.spaceeye.vmod.compat.schem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.schematic.MVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip

class CreateContraptionsCompat: SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}
    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((BlockEntity?) -> Unit) -> Unit) {}

    override fun onEntityCopy(level: ServerLevel, entity: Entity, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {
        if (!tag.contains("Contraption") || !tag.getCompound("Contraption").contains("Anchor")) {return}

        val contraptionTag = tag.getCompound("Contraption")
        val anchorTag = contraptionTag.getCompound("Anchor")

        val pos = MVector3d(anchorTag.getInt("X"), anchorTag.getInt("Y"), anchorTag.getInt("Z"))
        val newPos = pos - shipCenter

        anchorTag.putInt("X", newPos.x.toInt())
        anchorTag.putInt("Y", newPos.y.toInt())
        anchorTag.putInt("Z", newPos.z.toInt())
    }

    override fun onEntityPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {
        if (!tag.contains("Contraption") || !tag.getCompound("Contraption").contains("Anchor")) {return}

        val contraptionTag = tag.getCompound("Contraption")
        val anchorTag = contraptionTag.getCompound("Anchor")

        val oldPos = MVector3d(anchorTag.getInt("X"), anchorTag.getInt("Y"), anchorTag.getInt("Z"))
        val newPos = oldPos + shipCenter

        anchorTag.putInt("X", newPos.x.toInt())
        anchorTag.putInt("Y", newPos.y.toInt())
        anchorTag.putInt("Z", newPos.z.toInt())
    }
}