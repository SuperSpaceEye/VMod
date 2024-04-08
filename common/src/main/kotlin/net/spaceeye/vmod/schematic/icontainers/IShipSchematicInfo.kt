package net.spaceeye.vmod.schematic.icontainers

import org.joml.Vector3d
import org.joml.primitives.AABBdc

interface IShipSchematicInfo {
    val worldBounds: AABBdc

    val shipInfo: List<IShipInfo>
}