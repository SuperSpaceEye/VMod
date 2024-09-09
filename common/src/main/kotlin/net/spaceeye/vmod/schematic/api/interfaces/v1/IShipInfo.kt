package net.spaceeye.vmod.schematic.api.interfaces.v1

import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.primitives.AABBic

interface IShipInfo {
    /**
     * Doesn't need to represent actual id. It's just needed to differentiate saved ships
     */
    val id: Long

    /**
     * Position of this ship relative to center of all ships inside of schematic
     */
    val relPositionToCenter: Vector3d

    /**
     * Ship's shipAABB but with min shipyard pos subtracted to it's within [0, 4095]
     */
    val shipAABB: AABBic

    /**
     * Ship's positionInShip but with min shipyard pos subtracted to it's within [0, 4095]
     */
    val positionInShip: Vector3d

    val shipScale: Double

    /**
     * Rotation of ship assuming the object's quaternion is Quaternion(x=0, y=0, z=0, w=1)
     */
    val rotation: Quaterniondc
}