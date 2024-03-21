package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag

fun serializeBlockPositions(items: List<BlockPos>): ListTag {
    val listTag = ListTag()
    items.forEach {
        val item = CompoundTag()
        item.putInt("x", it.x)
        item.putInt("y", it.y)
        item.putInt("z", it.z)
        listTag.add(item)
    }
    return listTag
}

fun deserializeBlockPositions(tag: Tag): List<BlockPos> {
    val points = mutableListOf<BlockPos>()
    (tag as ListTag).forEach {
        it as CompoundTag
        points.add(BlockPos(it.getInt("x"), it.getInt("y"), it.getInt("z")))
    }
    return points
}