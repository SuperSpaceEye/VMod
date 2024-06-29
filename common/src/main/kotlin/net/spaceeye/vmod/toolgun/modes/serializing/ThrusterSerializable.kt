package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode

interface ThrusterSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as ThrusterMode
        val buf = getBuffer()

        buf.writeUtf(channel)
        buf.writeDouble(force)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as ThrusterMode

        channel = buf.readUtf()
        force = buf.readDouble()
    }
    override fun serverSideVerifyLimits() {
    }
}