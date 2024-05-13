package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

fun rotateAroundCenter(center: ShipTransform, obj: ShipTransform, rotation: Quaterniond): ShipTransform {
    center as ShipTransformImpl
    obj as ShipTransformImpl

    val objPos = Vector3d(obj.positionInWorld)
    val objRot = obj.shipToWorldRotation

    val shipPosInCenterShipyard = posWorldToShip(null, objPos, center)

    val rotatedCenter = center.copy(shipToWorldRotation = center.shipToWorldRotation.mul(rotation, Quaterniond()))

    val diff = rotatedCenter.shipToWorldRotation.mul(center.shipToWorldRotation.invert(Quaterniond()), Quaterniond())

    val newRotation = diff.mul(objRot)
    val newPos = posShipToWorld(null, shipPosInCenterShipyard, rotatedCenter)

    return obj.copy(positionInWorld = newPos.toJomlVector3d(), shipToWorldRotation = newRotation)
}