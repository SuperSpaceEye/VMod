package net.spaceeye.vmod.schematic.containers

import net.spaceeye.vmod.schematic.icontainers.IShipInfo
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.primitives.AABBic

class ShipInfo(
    override val id: Long,
    override val relPositionToCenter: Vector3d,
    override val shipBounds: AABBic,
    override val posInShip: Vector3d,
    override val shipScale: Double,
    override val rotation: Quaterniondc
) : IShipInfo