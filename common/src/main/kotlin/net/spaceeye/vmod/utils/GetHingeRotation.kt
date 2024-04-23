package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import kotlin.math.abs
import kotlin.math.sqrt

//https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
fun getHingeRotation(transform: ShipTransform?, worldDirection: Vector3d): Quaterniond {
    val right = Vector3d(1, 0, 0)

    val localDir = if (transform != null) {
        Vector3d(transform.transformDirectionNoScalingFromWorldToShip(worldDirection.toJomlVector3d(), JVector3d()))
    } else {
        worldDirection
    }

    if ((localDir - right).dist() < 1e-5) { return Quaterniond() }

    val v1l = right.dist()
    val v2l = localDir.dist()

    val a = right.cross(localDir)

    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)

    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.toJomlVector3d().orthogonalize(right.toJomlVector3d())
        return Quaterniond(ort.x, ort.y, ort.z, 0.0)
    }

    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize().invert()
}