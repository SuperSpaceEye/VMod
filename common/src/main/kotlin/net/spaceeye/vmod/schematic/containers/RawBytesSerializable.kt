package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable

class RawBytesSerializable(val bytes: ByteArray): Serializable {
    override fun serialize(): FriendlyByteBuf { return FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)) }
    override fun deserialize(buffer: FriendlyByteBuf) { throw AssertionError("Not Implemented. Not going to be implemented.") }
}