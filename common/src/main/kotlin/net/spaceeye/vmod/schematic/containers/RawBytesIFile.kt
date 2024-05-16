package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.spaceeye.vmod.schematic.icontainers.IFile

class RawBytesIFile(val bytes: ByteArray): IFile {
    override fun toBytes(): ByteBuf {
        return Unpooled.wrappedBuffer(bytes)
    }

    override fun fromBytes(buffer: ByteBuf): Boolean {
        throw AssertionError("Not Implemented. Not going to be implemented.")
    }
}