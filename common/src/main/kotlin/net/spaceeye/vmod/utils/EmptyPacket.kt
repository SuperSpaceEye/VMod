package net.spaceeye.vmod.utils

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable

class EmptyPacket: Serializable {
    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}
}