package net.spaceeye.vmod.compat.schem

import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.constraintsManaging.getCenterPos
import net.spaceeye.vmod.utils.Vector3d
import net.takeoff.TakeoffBlocks
import net.takeoff.blockentity.BearingBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class TakeoffSchemCompat: SchemCompatItem {
    init {
        PlayerEvent.PLAYER_JOIN.register {
            it.sendMessage(TextComponent("Warning! VMod has detected takeoff and added schem compatibility for its bearing. " +
                    "IF YOU COPY A STRUCTURE WITH TAKEOFF BEARING, MAKE SURE THAT EVERYTHING COPIED CORRECTLY! " +
                    "For some reason bearing's \"Top\" doesn't add to ships AABB, making it not copy the tops. " +
                    "While an incorrectly copied bearing will work, it WILL BRICK your server after you load your world again, so BE WARNED. " +
                    "Also, because takeoff is so old, I WILL BE FIXING THIS. It will also be probably be removed in the future, idk."), it.uuid)
        }
    }

    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?) {
        if (   state.block != TakeoffBlocks.BEARING.get()
            && state.block != TakeoffBlocks.BEARING_TOP.get()
        ) { return }
        if (tag == null) {return}

        tag.putLong("VMOD_INJECT_otherId", level.getShipManagingPos(BlockPos.of(tag.getLong("otherPos")))?.id ?: return)
    }

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (   state.block != TakeoffBlocks.BEARING.get()
            && state.block != TakeoffBlocks.BEARING_TOP.get()
            ) { return }
        if (!tag.contains("VMOD_INJECT_otherId")) {return}

        val newOtherShip = level.shipObjectWorld.allShips.getById(oldToNewId[tag.getLong("VMOD_INJECT_otherId")]!!)!!

        val oldPos = Vector3d(BlockPos.of(tag.getLong("otherPos")))
        val newPos = oldPos - getCenterPos(oldPos.x.toInt(), oldPos.z.toInt()) + getCenterPos(newOtherShip.chunkClaim.xMiddle * 16, newOtherShip.chunkClaim.zMiddle * 16)

        tag.putLong("otherPos", newPos.toBlockPos().asLong())

        afterPasteCallbackSetter {
            it as BearingBlockEntity
            it.createConstraints(newOtherShip)
        }
    }
}