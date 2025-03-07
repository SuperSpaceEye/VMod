package net.spaceeye.vmod.utils.vs

import org.joml.Quaterniond
import org.joml.Vector3d
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

object transformF {
    fun createD(position: Vector3d? = null, rotation: Quaterniond? = null, scaling: Vector3d? = null, shipPos: Vector3d? = null) = ShipTransformImpl.createEmpty().createD(position, rotation, scaling, shipPos)
}

@OptIn(VsBeta::class)
fun BodyTransform.createD(position: Vector3d? = null, rotation: Quaterniond? = null, scaling: Vector3d? = null, shipPos: Vector3d? = null) = this.create(position ?: Vector3d(), rotation ?: Quaterniond(), scaling ?: Vector3d(1.0, 1.0, 1.0), shipPos ?: Vector3d())

fun BodyTransform.toShipTransform(): ShipTransform = ShipTransformImpl.create(this.position, this.positionInModel, this.rotation, this.scaling)