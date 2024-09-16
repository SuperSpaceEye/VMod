package net.spaceeye.vmod.schematic.api.containers

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable
import java.io.IOException

class CompoundTagSerializable(var tag: CompoundTag? = null): Serializable {
    override fun serialize(): FriendlyByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        NbtIo.writeCompressed(tag!!, buffer)
        //Is it efficient? No. But do i care? Also no.
        return FriendlyByteBuf(Unpooled.wrappedBuffer(FriendlyByteBuf(buffer.buffer()).accessByteBufWithCorrectSize()))
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        val buffer = ByteBufInputStream(buf)
        try {
            tag = NbtIo.readCompressed(buffer)
        } catch (e: IOException) {}
    }
}