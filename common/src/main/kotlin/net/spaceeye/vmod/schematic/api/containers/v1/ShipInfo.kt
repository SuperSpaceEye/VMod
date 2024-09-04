package net.spaceeye.vmod.schematic.api.containers.v1

import net.spaceeye.vmod.schematic.api.interfaces.v1.IShipInfo
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.primitives.AABBic

open class ShipInfo(
    override val id: Long,
    override val relPositionToCenter: Vector3d,
    override val shipAABB: AABBic,
    override val positionInShip: Vector3d,
    override val shipScale: Double,
    override val rotation: Quaterniondc
) : IShipInfo