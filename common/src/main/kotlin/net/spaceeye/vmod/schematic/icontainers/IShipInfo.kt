package net.spaceeye.vmod.schematic.icontainers

import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.primitives.AABBdc
import org.joml.primitives.AABBic

interface IShipInfo {
    val id: Long
    val relPositionToCenter: Vector3d
    val shipBounds: AABBic
    val worldBounds: AABBdc

    val posInShip: Vector3d

    val shipScale: Double
    val rotation: Quaterniondc
}