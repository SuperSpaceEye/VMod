package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable

interface StripSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}
    override fun serverSideVerifyLimits() {}
}