package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesSerializable

interface ThrusterSerializable: MSerializable, PlacementModesSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as ThrusterMode
        val buf = getBuffer()

        buf.writeUtf(channel)
        buf.writeDouble(force)

        pmSerialize(buf)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as ThrusterMode

        channel = buf.readUtf()
        force = buf.readDouble()

        pmDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        pmServerSideVerifyLimits()
    }
}