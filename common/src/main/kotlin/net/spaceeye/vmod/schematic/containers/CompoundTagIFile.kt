package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.spaceeye.vmod.schematic.icontainers.IFile
import java.io.IOException

class CompoundTagIFile(var tag: CompoundTag): IFile {
    override fun toBytes(): ByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        NbtIo.writeCompressed(tag, buffer)
        return buffer.buffer()
    }

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