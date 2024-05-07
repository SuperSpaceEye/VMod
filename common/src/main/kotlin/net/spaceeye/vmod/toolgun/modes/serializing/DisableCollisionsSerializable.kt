package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.DisableCollisionsMode

interface DisableCollisionsSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as DisableCollisionsMode
        val buf = getBuffer()

        buf.writeBoolean(primaryFirstRaycast)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as DisableCollisionsMode

        primaryFirstRaycast = buf.readBoolean()
    }
    override fun serverSideVerifyLimits() {}
}