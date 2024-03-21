package net.spaceeye.vsource.utils

import net.minecraft.core.BlockPos

typealias DataItem<T> = MutableList<T>
typealias DataMap<T> = MutableMap<Int, MutableMap<Int, MutableMap<Int, DataItem<T>>>>

class PosMap<T> {
    // y, x, z
    private val yxz: DataMap<T> = mutableMapOf()

    fun addItemTo(item: T, pos: BlockPos) {
        yxz.getOrPut(pos.y) { mutableMapOf() }
            .getOrPut(pos.x) { mutableMapOf() }
            .getOrPut(pos.z) { mutableListOf()}
            .add(item)
    }

    fun getItemsAt(pos: BlockPos) = yxz.get(pos.y)?.get(pos.x)?.get(pos.z)
    fun getItemsAt(x: Int, y: Int, z: Int) = yxz.get(y)?.get(x)?.get(z)

    fun removeItemFromPos(item: T, pos: BlockPos): Boolean {
        val xz = yxz[pos.y] ?: return false
        val  z =  xz[pos.x] ?: return false
        val it =   z[pos.z] ?: return false
        if (!it.remove(item)) { return false }

        if (it.isNotEmpty()) { return true }
        z.remove(pos.z)
        if (z.isNotEmpty())  { return true }
        xz.remove(pos.x)
        if (xz.isNotEmpty()) { return true }
        yxz.remove(pos.y)
        return true
    }

    fun clear() {
        yxz.clear()
    }
}