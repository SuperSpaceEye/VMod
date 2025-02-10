package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos
import org.joml.Vector3i

typealias DataItem<T> = MutableList<T>
typealias DataMap<T> = MutableMap<Int, MutableMap<Int, MutableMap<Int, T>>>

class PosMapList<T> {
    // y, x, z
    private val yxz: DataMap<DataItem<T>> = mutableMapOf()

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

class PosMap<T> {
    // y, x, z
    private val yxz: DataMap<T> = mutableMapOf()

    fun setItemTo(item: T, x: Int, y: Int, z: Int) {
        yxz.getOrPut(y) { mutableMapOf() }
           .getOrPut(x) { mutableMapOf() }[z] = item
    }
    fun setItemTo(item: T, pos: BlockPos) {
        yxz.getOrPut(pos.y) { mutableMapOf() }
           .getOrPut(pos.x) { mutableMapOf() }[pos.z] = item
    }

    fun getItemAt(pos: BlockPos) = yxz.get(pos.y)?.get(pos.x)?.get(pos.z)
    fun getItemAt(x: Int, y: Int, z: Int) = yxz.get(y)?.get(x)?.get(z)

    fun removeItemFromPos(xPos: Int, yPos: Int, zPos: Int): Boolean {
        val xz = yxz[yPos] ?: return false
        val  z =  xz[xPos] ?: return false
        z.remove(zPos)

        if (z.isNotEmpty())  { return true }
        xz.remove(xPos)
        if (xz.isNotEmpty()) { return true }
        yxz.remove(yPos)
        return true
    }

    fun asList() = yxz
        .map { (y, xz) -> xz.map { (x, zM) -> zM.map { (z, mass) -> Pair(Vector3i(x, y, z), mass) } } }
        .flatten()
        .flatten()

    fun clear() {
        yxz.clear()
    }
}