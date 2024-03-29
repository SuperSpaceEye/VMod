package net.spaceeye.vmod.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.VMBlockEntities
import net.spaceeye.vmod.network.Activate
import net.spaceeye.vmod.network.Deactivate
import net.spaceeye.vmod.network.Message

class SimpleMessagerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(VMBlockEntities.SIMPLE_MESSAGER.get(), pos, state) {
    var channel = "hydraulics"
    var msg: Message = Activate()

    override fun load(tag: CompoundTag) {
        super.load(tag)
        channel = tag.getString("channel")
        msg = when(tag.getBoolean("msg")) {
            true -> Activate()
            false -> Deactivate()
        }
    }
    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putString("channel", channel)
        tag.putBoolean("msg", when (msg) {
            is Activate -> true
            else -> false
        })
    }
}