package net.spaceeye.vmod.schematic.api.interfaces

import io.netty.buffer.ByteBuf
import net.spaceeye.vmod.networking.Serializable

interface IShipSchematic {
    fun getInfo(): IShipSchematicInfo

    fun serialize(): Serializable
    fun deserialize(buf: ByteBuf): Boolean
}