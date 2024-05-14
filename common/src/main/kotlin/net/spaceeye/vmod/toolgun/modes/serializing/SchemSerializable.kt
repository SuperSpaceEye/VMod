package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.SchemMode

interface SchemSerializable: MSerializable {
    override fun serverSideVerifyLimits() {}
    override fun serialize(): FriendlyByteBuf {
        this as SchemMode
        val buf = getBuffer()

        buf.writeDouble(rotationAngle.it)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as SchemMode
        rotationAngle.it = buf.readDouble()
    }
}