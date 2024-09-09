package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable
import java.io.IOException

class CompoundSerializableWithTopVersion(var tag: CompoundTag, val version: Int): Serializable {
    override fun serialize(): FriendlyByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        buffer.writeInt(version)
        NbtIo.writeCompressed(tag, buffer)
        return FriendlyByteBuf(buffer.buffer())
    }

    //TODO figure out wtf i meant here

    // version was already written before calling fromBytes
    override fun deserialize(buf: FriendlyByteBuf) {
        val buffer = ByteBufInputStream(buf)
        try {
            tag = NbtIo.readCompressed(buffer)
        } catch (e: IOException) {}
    }
}