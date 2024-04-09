package net.spaceeye.vmod.schematic.icontainers

import net.minecraft.server.level.ServerLevel
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip

interface IShipSchematic {
    val schematicVersion: Int

    fun getInfo(): IShipSchematicInfo

    fun placeAt(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc): Boolean
    fun makeFrom(originShip: ServerShip): Boolean

    fun saveToFile(): IFile
    fun loadFromFile(file: IFile): Boolean
}