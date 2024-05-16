package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.spaceeye.vmod.schematic.icontainers.IFile
import java.io.IOException

class CompoundTagIFileWithTopVersion(var tag: CompoundTag, val version: Int): IFile {
    override fun toBytes(): ByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        buffer.writeInt(version)
        NbtIo.writeCompressed(tag, buffer)
        return buffer.buffer()
    }

    // version was already written before calling fromBytes
    override fun fromBytes(buffer: ByteBuf): Boolean {
        val _buffer = ByteBufInputStream(buffer)
        try {
            tag = NbtIo.readCompressed(_buffer)
        } catch (e: IOException) {
            return false
        }

        return true
    }
}