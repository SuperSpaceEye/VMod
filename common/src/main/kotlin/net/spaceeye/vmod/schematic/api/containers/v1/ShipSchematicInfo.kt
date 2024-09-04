package net.spaceeye.vmod.schematic.api.containers.v1

import net.spaceeye.vmod.schematic.api.interfaces.IShipSchematicInfo
import net.spaceeye.vmod.schematic.api.interfaces.v1.IShipInfo
import org.joml.Vector3d

open class ShipSchematicInfo(
    override val maxObjectPos: Vector3d,
    override var shipsInfo: List<IShipInfo>
) : IShipSchematicInfo