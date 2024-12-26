package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos

inline fun BlockPos(x: Double, y: Double, z: Double) = BlockPos(x.toInt(), y.toInt(), z.toInt())