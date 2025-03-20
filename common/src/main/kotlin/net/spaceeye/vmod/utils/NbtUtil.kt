package net.spaceeye.vmod.utils

import net.minecraft.nbt.CompoundTag
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import java.awt.Color

fun CompoundTag.putQuatd(prefix: String, quaterniond: Quaterniondc) =
    with(quaterniond) {
        putDouble(prefix + "w", w())
        putDouble(prefix + "x", x())
        putDouble(prefix + "y", y())
        putDouble(prefix + "z", z())
    }

fun CompoundTag.getQuatd(prefix: String): Quaterniond? {
    return if (
        !contains(prefix + "w") ||
        !contains(prefix + "x") ||
        !contains(prefix + "y") ||
        !contains(prefix + "z")
    ) {
        null
    } else {
        Quaterniond(
            getDouble(prefix + "x"),
            getDouble(prefix + "y"),
            getDouble(prefix + "z"),
            getDouble(prefix + "w"),
        )
    }
}

fun CompoundTag.putVector3d(prefix: String, vector3d: Vector3dc) =
    with(vector3d) {
        putDouble(prefix + "x", x())
        putDouble(prefix + "y", y())
        putDouble(prefix + "z", z())
    }

fun CompoundTag.getVector3d(prefix: String): Vector3d? {
    return if (
        !contains(prefix + "x") ||
        !contains(prefix + "y") ||
        !contains(prefix + "z")
    ) {
        null
    } else {
        Vector3d(
            getDouble(prefix + "x"),
            getDouble(prefix + "y"),
            getDouble(prefix + "z")
        )
    }
}

fun CompoundTag.putMyVector3d(prefix: String, vector3d: net.spaceeye.vmod.utils.Vector3d) =
    with(vector3d) {
        putDouble(prefix + "x", x)
        putDouble(prefix + "y", y)
        putDouble(prefix + "z", z)
    }

fun CompoundTag.getMyVector3d(prefix: String): net.spaceeye.vmod.utils.Vector3d {
    return net.spaceeye.vmod.utils.Vector3d(
        getDouble(prefix + "x"),
        getDouble(prefix + "y"),
        getDouble(prefix + "z")
    )
}

fun CompoundTag.putColor(key: String, color: Color) = putInt(key, color.rgb)
fun CompoundTag.getColor(key: String) = Color(getInt(key), true)