package net.spaceeye.vmod.schematic.icontainers

import org.joml.primitives.AABBdc

interface IShipSchematicInfo {
    val worldBounds: AABBdc

    var shipInfo: List<IShipInfo>
}