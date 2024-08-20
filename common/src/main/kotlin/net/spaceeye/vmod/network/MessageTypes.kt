package net.spaceeye.vmod.network

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.utils.Registry

object MessageTypes: Registry<Message>(false) {
    init {
        register(Signal::class)
    }

    fun serialize(msg: Message): CompoundTag {
        val toReturn = CompoundTag()

        toReturn.putString("type", typeToString(msg::class.java))
        toReturn.put("data", msg.toNBT())

        return toReturn
    }

    fun deserialize(tag: CompoundTag): Message {
        val item = strTypeToSupplier(tag.getString("type")).get()
        item.fromNBT(tag.getCompound("data"))
        return item
    }
}

interface Message {
    fun toNBT(): CompoundTag
    fun fromNBT(tag: CompoundTag)
}

class Signal(): Message {
    var percentage = 0.0
    constructor(percentage: Double) : this() {this.percentage = percentage}

    override fun toNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putDouble("percentage", percentage)
        return tag
    }

    override fun fromNBT(tag: CompoundTag) {
        percentage = tag.getDouble("percentage")
    }
}