package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode

interface ScaleSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as ScaleMode
        val buf = getBuffer()

        buf.writeDouble(scale)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as ScaleMode
        scale = buf.readDouble()
    }

    override fun serverSideVerifyLimits() {
        this as ScaleMode
        scale = ServerLimits.instance.scale.get(scale)
    }
}