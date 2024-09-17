package net.spaceeye.vmod.schematic.interfaces

import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import java.util.*

//TODO rename
interface SchemPlaceAtMakeFrom: IShipSchematic, IShipSchematicDataV1 {
    var schemInfo: IShipSchematicInfo?

    fun placeAt(level: ServerLevel, uuid: UUID, pos: Vector3d, rotation: Quaterniondc, postPlaceFn: (List<ServerShip>) -> Unit = {}): Boolean
    fun makeFrom(level: ServerLevel, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit = {}): Boolean
}