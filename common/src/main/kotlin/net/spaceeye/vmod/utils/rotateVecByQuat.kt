package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d

fun rotateVecByQuat(vec: Vector3d, quaterniond: Quaterniondc): Vector3d {
    val P = Quaterniond(vec.x, vec.y, vec.z, 0.0)
    val R = quaterniond
    val RI = R.invert(Quaterniond())

    val res = R.mul(P, Quaterniond()).mul(RI)

    return Vector3d(res.x, res.y, res.z)
}