package net.spaceeye.vmod.network

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.TagAutoSerializable
import net.spaceeye.vmod.reflectable.TagSerializable
import net.spaceeye.vmod.utils.Registry

object MessageTypes: Registry<Message>(false) {
    init {
        register(Signal::class)
        register(AngularSpeedSignal::class)
    }

    fun serialize(msg: Message): CompoundTag {
        val toReturn = CompoundTag()

        toReturn.putString("type", typeToString(msg::class.java))
        toReturn.put("data", msg.tSerialize())

        return toReturn
    }

    fun deserialize(tag: CompoundTag): Message {
        val item = strTypeToSupplier(tag.getString("type")).get()
        item.tDeserialize(tag.getCompound("data"))
        return item
    }
}

interface Message: TagSerializable

//TODO auto serializable?
class Signal(): Message, TagAutoSerializable {
    var percentage: Double by get(0, 0.0)
    constructor(percentage: Double) : this() {this.percentage = percentage}
}

class AngularSpeedSignal(): Message, TagAutoSerializable {
    var angularSpeed: Double by get(0, 0.0)
}