package net.spaceeye.vssource.utils

import net.minecraft.nbt.CompoundTag
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc

fun CompoundTag.putQuaterniond(prefix: String, quaterniond: Quaterniondc) =
    with(quaterniond) {
        putDouble(prefix + "w", w())
        putDouble(prefix + "x", x())
        putDouble(prefix + "y", y())
        putDouble(prefix + "z", z())
    }

fun CompoundTag.getQuaterniond(prefix: String): Quaterniond? {
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