package net.spaceeye.vmod.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.VMBlockEntities
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessageTypes
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal

class SimpleMessagerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(VMBlockEntities.SIMPLE_MESSAGER.get(), pos, state) {
    private var _transmit: Boolean = true
    private var _channel: String = "hydraulics"
    private var num = 0

    var channel
        get() = _channel
        set(value) {
            _channel = value
            num++
            if (!transmit) {
                initNetworkMaybe()
            }
        }
    var msg: Message = Signal()
    var transmit: Boolean
        get() = _transmit
        set(value) {
            _transmit = value
            if (level !is ServerLevel) {return}
            msg = Signal(1.0)
            level!!.updateNeighborsAt(blockPos, this.blockState.block)
            level!!.updateNeighbourForOutputSignal(blockPos, this.blockState.block)
        }
    var lastSignal: Int = 0


    init {
        initNetworkMaybe()
    }

    private fun initNetworkMaybe() {
        var control = num
        MessagingNetwork.register(channel) { msg, unregister ->
            if (num != control) { unregister(); return@register }
            if (msg !is Signal) {return@register}
            this.msg = msg
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        msg = MessageTypes.deserialize(tag.getCompound("msg"))
        lastSignal = tag.getInt("lastSignal")
        transmit = tag.getBoolean("transmit")
        channel = tag.getString("channel")
    }
    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("msg", MessageTypes.serialize(msg))
        tag.putInt("lastSignal", lastSignal)
        tag.putBoolean("transmit", transmit)
        tag.putString("channel", channel)
    }
}