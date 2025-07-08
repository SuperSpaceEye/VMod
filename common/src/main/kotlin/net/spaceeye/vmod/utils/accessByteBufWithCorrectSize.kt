package net.spaceeye.vmod.utils

import io.netty.buffer.ByteBuf

fun ByteBuf.accessByteBufWithCorrectSize(): ByteArray {
    return ByteArray(this.writerIndex()).also { this.getBytes(0, it) }
}