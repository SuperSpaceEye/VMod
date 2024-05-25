package net.spaceeye.vmod.schematic.icontainers

import io.netty.buffer.ByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.networking.Serializable
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import java.util.UUID

interface IShipSchematic {
    val schematicVersion: Int

    fun getInfo(): IShipSchematicInfo

    fun placeAt(level: ServerLevel, uuid: UUID, pos: Vector3d, rotation: Quaterniondc): Boolean
    fun makeFrom(level: ServerLevel, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit = {}): Boolean

    fun saveToFile(): Serializable
    fun loadFromByteBuffer(buf: ByteBuf): Boolean
}