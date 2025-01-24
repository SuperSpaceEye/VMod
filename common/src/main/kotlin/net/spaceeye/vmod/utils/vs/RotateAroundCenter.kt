package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.BodyTransform
import org.valkyrienskies.core.api.bodies.properties.rebuild
import org.valkyrienskies.core.api.ships.properties.ShipTransform

// does quaternion magic to rotate object around a center by given rotation
@OptIn(VsBeta::class)
fun rotateAroundCenter(center: BodyTransform, obj: ShipTransform, rotation: Quaterniondc): BodyTransform {

    val objPos = Vector3d(obj.positionInWorld)
    val objRot = obj.shipToWorldRotation

    val shipPosInCenterShipyard = posWorldToShip(null, objPos, center)

    val rotatedCenter = center.rebuild {
        this.rotation(center.rotation.mul(rotation, Quaterniond()))
    }

    val diff = rotatedCenter.rotation.mul(center.rotation.invert(Quaterniond()), Quaterniond())

    val newRotation = diff.mul(objRot)
    val newPos = posShipToWorld(null, shipPosInCenterShipyard, rotatedCenter)

    return obj.rebuild {
        this.position(newPos.toJomlVector3d())
        this.rotation(newRotation)
    }
}