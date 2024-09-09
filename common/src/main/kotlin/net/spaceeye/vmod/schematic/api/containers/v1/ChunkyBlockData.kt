package net.spaceeye.vmod.schematic.api.containers.v1

import net.minecraft.core.BlockPos

open class ChunkyBlockData<T>() {
    val blocks = mutableMapOf<BlockPos, MutableMap<BlockPos, T>>()
    val sortedChunkKeys = mutableListOf<BlockPos>()

    fun add(x: Int, y: Int, z: Int, item: T) {
        blocks.getOrPut(BlockPos(x shr 4, 0, z shr 4))
        { mutableMapOf() }[BlockPos(x and 15, y, z and 15)] = item
    }

    fun updateKeys() {
        sortedChunkKeys.clear()
        sortedChunkKeys.addAll(blocks.keys)
        sortedChunkKeys.sort()
    }

    inline fun chunkForEach(chunkNum: Int, fn: (x: Int, y: Int, z: Int, item: T) -> Unit) {
        val cpos = sortedChunkKeys[chunkNum]

        blocks[cpos]!!.forEach { (pos, item) -> fn(pos.x + (cpos.x shl 4), pos.y, pos.z + (cpos.z shl 4), item) }
    }

    inline fun forEach(fn: (x: Int, y: Int, z: Int, item: T) -> Unit) {
        blocks.forEach { (cpos, chunk) ->
            chunk.forEach { (pos, item) ->
                fn(pos.x + (cpos.x shl 4), pos.y, pos.z + (cpos.z shl 4), item)
            }
        }
    }
}