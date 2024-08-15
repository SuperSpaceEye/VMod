package net.spaceeye.vmod.utils

import org.joml.Quaterniond
import org.joml.Quaternionf

fun Quaternionf.toJoml(): Quaterniond {
    return Quaterniond(this.x.toDouble(), this.y.toDouble(), this.z.toDouble(), this.w.toDouble())
}