package net.spaceeye.vmod.utils

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.schematic.MVector3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import java.awt.Color

fun CompoundTag.putQuatd(prefix: String, quaterniond: Quaterniondc) =
    with(quaterniond) {
        put(prefix, CompoundTag().also { with(it) {
            putDouble("x", x())
            putDouble("y", y())
            putDouble("z", z())
            putDouble("w", w())
        } })
    }

fun CompoundTag.getQuatd(prefix: String): Quaterniond? {
    return if (contains(prefix)){
        with(get(prefix) as CompoundTag) {
            Quaterniond(
                getDouble("x"),
                getDouble("y"),
                getDouble("z"),
                getDouble("w"),
            )
        }
    // TODO remove eventually
    } else if (
        contains(prefix + "w") &&
        contains(prefix + "x") &&
        contains(prefix + "y") &&
        contains(prefix + "z")
    ) {
        Quaterniond(
            getDouble(prefix + "x"),
            getDouble(prefix + "y"),
            getDouble(prefix + "z"),
            getDouble(prefix + "w"),
        )
    } else {
        null
    }
}

fun CompoundTag.putVector3d(prefix: String, vector3d: Vector3dc) =
    with(vector3d) {
        put(prefix, CompoundTag().also { with(it) {
            putDouble("x", x())
            putDouble("y", y())
            putDouble("z", z())
        } })
    }

fun CompoundTag.getVector3d(prefix: String): Vector3d? {
    return if (contains(prefix)) {
        with(get(prefix) as CompoundTag) {
            Vector3d(
                getDouble("x"),
                getDouble("y"),
                getDouble("z")
            )
        }
        //TODO remove eventually
    } else if (
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

fun CompoundTag.putMyVector3d(prefix: String, vector3d: MVector3d) =
    with(vector3d) {
        put(prefix, CompoundTag().also { with(it) {
            putDouble("x", x)
            putDouble("y", y)
            putDouble("z", z)
        } })
    }

fun CompoundTag.getMyVector3d(prefix: String): MVector3d {
    return if (contains(prefix)) {
        with(get(prefix) as CompoundTag) {
            MVector3d(
                getDouble("x"),
                getDouble("y"),
                getDouble("z")
            )
        }
        //TODO remove eventually
    } else {
        MVector3d(
            getDouble(prefix + "x"),
            getDouble(prefix + "y"),
            getDouble(prefix + "z")
        )
    }
}

fun CompoundTag.putColor(key: String, color: Color) = putInt(key, color.rgb)
fun CompoundTag.getColor(key: String) = Color(getInt(key), true)