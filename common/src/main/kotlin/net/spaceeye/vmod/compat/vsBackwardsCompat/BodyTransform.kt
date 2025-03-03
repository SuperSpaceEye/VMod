package net.spaceeye.vmod.compat.vsBackwardsCompat

import org.joml.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias BodyTransform = ShipTransform

val BodyTransform.position: Vector3dc get() = this.positionInWorld
val BodyTransform.rotation: Quaterniondc get() = this.shipToWorldRotation
val BodyTransform.scaling: Vector3dc get() = this.shipToWorldScaling
val BodyTransform.positionInModel: Vector3dc get() = this.positionInShip
val BodyTransform.toWorld: Matrix4dc get() = this.shipToWorld
val BodyTransform.toModel: Matrix4dc get() = this.worldToShip

fun BodyTransform.toBuilder(): BodyTransformBuilder {return BodyTransformBuilder().from(this)}
fun BodyTransform.create(position: Vector3dc, rotation: Quaterniondc, positionInModel: Vector3dc, scaling: Vector3dc): BodyTransform = ShipTransformImpl.create(position, positionInModel, rotation, scaling)

class BodyTransformBuilder {
    val position: Vector3d = Vector3d()
    val positionInModel: Vector3d = Vector3d()
    val rotation: Quaterniond = Quaterniond()
    val scaling: Vector3d = Vector3d()

    fun position(position: Vector3dc): BodyTransformBuilder = also {
        this.position.set(position)
    }

    fun positionInModel(positionInModel: Vector3dc): BodyTransformBuilder = also {
        this.positionInModel.set(positionInModel)
    }

    fun rotation(rotation: Quaterniondc): BodyTransformBuilder = also {
        this.rotation.set(rotation)
    }

    fun scaling(scaling: Vector3dc): BodyTransformBuilder = also {
        this.scaling.set(scaling)
    }

    /**
     * Set this builder's fields to those of [transform]
     *
     * @see BodyTransform.toBuilder
     */
    fun from(transform: BodyTransform): BodyTransformBuilder {
        position(transform.position.get(Vector3d()))
        positionInModel(transform.positionInModel.get(Vector3d()))
        rotation(transform.rotation.get(Quaterniond()))
        scaling(transform.scaling.get(Vector3d()))
        return this
    }

    /**
     * Instantiates a new [BodyTransform] with the given [position], [positionInModel], [rotation], and [scaling].
     */
    fun build(): BodyTransform {
        return ShipTransformImpl(position, positionInModel, rotation, scaling)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun BodyTransform.rebuild(block: BodyTransformBuilder.() -> Unit): BodyTransform {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.toBuilder().apply(block).build()
}