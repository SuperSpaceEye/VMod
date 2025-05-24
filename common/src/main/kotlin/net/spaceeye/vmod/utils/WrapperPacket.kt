package net.spaceeye.vmod.utils

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable

class WrapperPacket(): Serializable {
    private lateinit var item: Serializable
    lateinit var buf: FriendlyByteBuf

    constructor(item: Serializable): this() { this.item = item }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeByteArray(item.serialize().accessByteBufWithCorrectSize())

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this.buf = FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))
    }
}