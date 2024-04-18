package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.StripMode

interface StripSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as StripMode
        val buf = getBuffer()

        buf.writeEnum(mode)
        buf.writeInt(radius)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as StripMode

        mode = buf.readEnum(mode.javaClass)
        radius = buf.readInt()
    }
    override fun serverSideVerifyLimits() {
        this as StripMode
        radius = ServerLimits.instance.stripRadius.get(radius)
    }
}