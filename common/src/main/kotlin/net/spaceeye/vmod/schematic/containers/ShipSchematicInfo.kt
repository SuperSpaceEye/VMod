package net.spaceeye.vmod.schematic.containers

import net.spaceeye.vmod.schematic.icontainers.IShipInfo
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import org.joml.Vector3d

class ShipSchematicInfo(
    override val maxObjectEdge: Vector3d,
    override var shipInfo: List<IShipInfo>) : IShipSchematicInfo