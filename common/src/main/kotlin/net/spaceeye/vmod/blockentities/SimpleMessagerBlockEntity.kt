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
    private var _transmit: Boolean = false
    private var _channel: String = "sensor"
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
        println("$channel $control")
        MessagingNetwork.register(channel) { msg, unregister ->
            if (num != control) { unregister(); return@register }
            if (msg !is Signal) {return@register}
            this.msg = msg
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        channel = tag.getString("channel")
        msg = MessageTypes.deserialize(tag.getCompound("msg"))
        transmit = tag.getBoolean("transmit")
        lastSignal = tag.getInt("lastSignal")
    }
    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putString("channel", channel)
        tag.put("msg", MessageTypes.serialize(msg))
        tag.putBoolean("transmit", transmit)
        tag.putInt("lastSignal", lastSignal)
    }
}