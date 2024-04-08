package net.spaceeye.vmod.schematic.icontainers

import java.nio.ByteBuffer

interface IFile {
    fun toBytes(): ByteBuffer
    fun fromBytes(buffer: ByteBuffer): Boolean
}