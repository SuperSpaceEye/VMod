package net.spaceeye.vmod.utils

import io.netty.buffer.ByteBuf
import net.minecraft.network.FriendlyByteBuf
import java.awt.Color

inline fun FriendlyByteBuf.writeColor(color: Color): ByteBuf = writeInt(color.rgb)
inline fun FriendlyByteBuf.readColor(): Color = Color(readInt(), true)