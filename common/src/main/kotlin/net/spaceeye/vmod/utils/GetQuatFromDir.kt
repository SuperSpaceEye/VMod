package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import kotlin.math.sqrt

// https://gamedev.stackexchange.com/questions/61672/align-a-rotation-to-a-direction
fun getQuatFromDir(dir: Vector3d): Quaterniond {
    //i'm not sure why up just like this works, but it does
    val up = Vector3d(0, 1, 0)
    val left = Vector3d(1, 0, 0)

    val a = up.cross(dir)
    val d = up.dot(dir)

    val q = if (d < 0.0 && a.sqrDist() <= 0.001) {
        Quaterniond(left.x, left.y, left.z, 0.0)
    } else {
        Quaterniond(a.x, a.y, a.z, sqrt(up.sqrDist() * dir.sqrDist()) + d)
    }
    return q.normalize()
}