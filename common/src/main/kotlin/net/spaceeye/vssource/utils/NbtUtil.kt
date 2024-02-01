package net.spaceeye.vssource.utils

import net.minecraft.nbt.CompoundTag
import org.joml.Quaterniond
import org.joml.Quaterniondc

fun CompoundTag.putQuaterniond(prefix: String, quaterniond: Quaterniondc) =
    with(quaterniond) {
        putDouble(prefix + "w", w())
        putDouble(prefix + "x", x())
        putDouble(prefix + "y", y())
        putDouble(prefix + "z", z())
    }

fun CompoundTag.getQuaterniond(prefix: String): Quaterniond? {
    return if (
        !prefix.contains(prefix + "w") ||
        !prefix.contains(prefix + "x") ||
        !prefix.contains(prefix + "y") ||
        !prefix.contains(prefix + "z")
    ) {
        null
    } else {
        Quaterniond(
            this.getDouble(prefix + "x"),
            this.getDouble(prefix + "y"),
            this.getDouble(prefix + "z"),
            this.getDouble(prefix + "w"),
        )
    }
}
