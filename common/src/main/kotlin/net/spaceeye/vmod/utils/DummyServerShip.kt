package net.spaceeye.vmod.utils

import org.joml.Vector3dc
import org.joml.primitives.AABBdc
import org.joml.primitives.AABBic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.*
import org.valkyrienskies.core.apigame.world.properties.DimensionId

// needed for CommonCopy
class DummyServerShip(override var activeChunksSet: IShipActiveChunksSet,
                      override var chunkClaim: ChunkClaim,
                      override var chunkClaimDimension: DimensionId,
                      override var id: ShipId,
                      override var omega: Vector3dc,
                      override var prevTickTransform: ShipTransform,
                      override var shipAABB: AABBic?,
                      override var slug: String?,
                      override var transform: ShipTransform,
                      override var velocity: Vector3dc,
                      override var worldAABB: AABBdc) : ServerShip {
    constructor(o: ServerShip): this(
            o.activeChunksSet,
            o.chunkClaim,
            o.chunkClaimDimension,
            o.id,
            o.omega,
            o.prevTickTransform,
            o.shipAABB,
            o.slug,
            o.transform,
            o.velocity,
            o.worldAABB
    )

    override var enableKinematicVelocity: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override val inertiaData: ShipInertiaData
        get() = TODO("Not yet implemented")
    override var isStatic: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var transformProvider: ServerShipTransformProvider?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun <T> getAttachment(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T> saveAttachment(clazz: Class<T>, value: T?) {
        TODO("Not yet implemented")
    }
}