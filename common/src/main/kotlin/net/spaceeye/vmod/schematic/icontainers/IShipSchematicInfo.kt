package net.spaceeye.vmod.schematic.icontainers

import org.joml.Vector3d

interface IShipSchematicInfo {
    val maxObjectWorldPos: Vector3d

    var shipInfo: List<IShipInfo>
}