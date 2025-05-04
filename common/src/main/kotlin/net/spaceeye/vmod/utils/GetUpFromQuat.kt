package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import org.joml.Quaterniondc

fun getUpFromQuat(quat1: Quaterniondc): Vector3d {
    val qrot = Quaterniond(quat1)
    val qup = Quaterniond(0.0, 1.0, 0.0, 0.0).premul(qrot).mul(qrot.conjugate())
    return Vector3d(qup.x, qup.y, qup.z).snormalize()
}