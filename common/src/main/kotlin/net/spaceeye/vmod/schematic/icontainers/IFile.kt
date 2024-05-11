package net.spaceeye.vmod.schematic.icontainers

import io.netty.buffer.ByteBuf

interface IFile {
    fun toBytes(): ByteBuf
    fun fromBytes(buffer: ByteBuf): Boolean
}