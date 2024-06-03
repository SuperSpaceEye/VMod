package net.spaceeye.vmod.schematic.icontainers

import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.primitives.AABBic

interface IShipInfo {
    val id: Long // doesn't represent actual id, needed just to differentiate ships inside the schem
    val relPositionToCenter: Vector3d // position relative to the center of the object
    val shipBounds: AABBic // shipyard bounds with min and max of [0, 4095]

    val posInShip: Vector3d // position in shipyard in boundary of [0, 4095]

    val shipScale: Double // only a double because scale being Vector3d in ship data is a lie and you can set a different scale for like x axis or smth
    val rotation: Quaterniondc
}