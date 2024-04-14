package net.spaceeye.vmod.network

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.utils.Registry
import net.spaceeye.vmod.utils.RegistryObject

object MessageTypes: Registry<Message>() {
    init {
        register(::Activate)
        register(::Deactivate)
        register(::Signal)
    }

    fun serialize(msg: Message): CompoundTag {
        val toReturn = CompoundTag()

        toReturn.putString("type", msg.typeName)
        toReturn.put("data", msg.toNBT())

        return toReturn
    }

    fun deserialize(tag: CompoundTag): Message {
        val item = typeToSupplier(tag.getString("type")).get()
        item.fromNBT(tag.getCompound("data"))
        return item
    }
}

interface Message: RegistryObject {
    fun toNBT(): CompoundTag
    fun fromNBT(tag: CompoundTag)
}

class Activate: Message {
    override fun toNBT(): CompoundTag { return CompoundTag() }
    override fun fromNBT(tag: CompoundTag) {}
    override val typeName = "Activate"
}

class Deactivate: Message {
    override fun toNBT(): CompoundTag { return CompoundTag() }
    override fun fromNBT(tag: CompoundTag) {}
    override val typeName = "Deactivate"
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

    override val typeName = "Signal"
}