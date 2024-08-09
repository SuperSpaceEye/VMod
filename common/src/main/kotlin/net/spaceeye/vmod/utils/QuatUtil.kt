package net.spaceeye.vmod.utils

import com.mojang.math.Quaternion
import org.joml.Quaterniond

fun Quaternion.toJoml(): Quaterniond {
    return Quaterniond(this.i().toDouble(), this.j().toDouble(), this.k().toDouble(), this.i().toDouble())
}