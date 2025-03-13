package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import kotlin.math.abs
import kotlin.math.sqrt

//https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
fun getHingeRotation(localDir: Vector3d, right: Vector3d = Vector3d(1, 0, 0)): Quaterniond {
    if ((localDir - right).dist() < 1e-5) { return Quaterniond() }

    val v1l = right.dist()
    val v2l = localDir.dist()

    val a = right.cross(localDir)

    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)

    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.toJomlVector3d().let { it.orthogonalize(it) }
        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
    }

    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
}