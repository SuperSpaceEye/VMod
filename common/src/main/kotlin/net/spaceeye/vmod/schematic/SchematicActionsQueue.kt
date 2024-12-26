package net.spaceeye.vmod.schematic

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.spaceeye.valkyrien_ship_schematics.SchematicRegistry
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockItem
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ChunkyBlockData
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableBlock
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.compat.schem.SchemCompatObj
import net.spaceeye.vmod.utils.BlockPos
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.getNow_ms
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.ships.ServerShip
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SchematicActionsQueue: ServerClosable() {
    init {
        //why here? idk
        SchematicRegistry.register(VModShipSchematicV1::class)
    }

    private val placeData = mutableMapOf<UUID, SchemPlacementItem>()
    private val saveData = mutableMapOf<UUID, SchemSaveItem>()
    private val unfreezeData = ConcurrentHashMap<UUID, SchemUnfreezeShips>()

    //why? If there are multiple schematics queued and there is a huge schematic it will not prevent other schematics
    // from being placed.
    private var placeLastKeys = placeData.keys.toList()
    private var placeLastPosition = 0

    private var saveLastKeys = placeData.keys.toList()
    private var saveLastPosition = 0

    fun uuidIsQueuedInSomething(uuid: UUID): Boolean = placeData.keys.contains(uuid) || saveData.keys.contains(uuid)

    fun queueShipsCreationEvent(level: ServerLevel, uuid: UUID, ships: List<Pair<ServerShip, Long>>, schematicV1: IShipSchematicDataV1, postPlacementFn: () -> Unit) {
        placeData[uuid] = SchemPlacementItem(level, schematicV1, ships, postPlacementFn)
    }

    class SchemPlacementItem(
        val level: ServerLevel,
        val schematicV1: IShipSchematicDataV1,
        val ships: List<Pair<ServerShip, Long>>,
        val postPlacementFn: () -> Unit
    ) {
        var currentShip = 0
        var currentChunk = 0

        var afterPasteCallbacks = mutableListOf<() -> Unit>()
        var delayedBlockEntityLoading = ChunkyBlockData<() -> Unit>()

        private fun placeChunk(level: ServerLevel, oldToNewId: Map<Long, Long>, currentChunkData: ChunkyBlockData<BlockItem>, blockPalette: IBlockStatePalette, flatTagData: List<CompoundTag>, offset: MVector3d) {
            currentChunkData.chunkForEach(currentChunk) { x, y, z, it ->
                val pos = MVector3d(x + offset.x, y + offset.y, z + offset.z).toBlockPos()
                val state = blockPalette.fromId(it.paletteId) ?: run {
                    throw RuntimeException("State under ID ${it.paletteId} is null.")
                }
                val block = state.block

                level.getChunkAt(pos).setBlockState(pos, state, false)
                if (block is ICopyableBlock) block.onPasteNoTag(level, pos, state, oldToNewId)
                if (it.extraDataId != -1) {
                    val tag = flatTagData[it.extraDataId]
                    tag.putInt("x", pos.x)
                    tag.putInt("y", pos.y)
                    tag.putInt("z", pos.z)

                    var delayLoading = true
                    var bcb: ((BlockEntity?) -> Unit)? = null
                    val cb = SchemCompatObj.onPaste(level, oldToNewId, tag, state) { delayLoading = it }
                    if (block is ICopyableBlock) block.onPaste(level, pos, state, oldToNewId, tag, { delayLoading = it }) { bcb = it }

                    val fn = {
                        val be = level.getChunkAt(pos).getBlockEntity(pos)
                        be?.load(tag) ?: run {
                            ELOG("$pos is not a block entity while data says otherwise. It can cause problems.")
                        }
                        cb?.let  { afterPasteCallbacks.add { it(be) } }
                        bcb?.let { afterPasteCallbacks.add { it(be) } }
                        Unit
                    }

                    if (delayLoading) delayedBlockEntityLoading.add(pos.x, pos.y, pos.z, fn) else fn()
                }
            }
        }

        private fun updateChunk(level: ServerLevel, currentChunkData: ChunkyBlockData<BlockItem>, offset: MVector3d) {
            currentChunkData.chunkForEach(currentChunk) { x, y, z, it ->
                val pos = BlockPos(x + offset.x, y + offset.y, z + offset.z)
                level.chunkSource.blockChanged(pos)
            }
        }

        fun place(start: Long, timeout: Long): Boolean {
            while (currentShip < ships.size) {
                val ship = ships[currentShip].first
                val currentBlockData = schematicV1.blockData[ships[currentShip].second] ?: throw RuntimeException("BLOCK DATA IS NULL. SHOULDN'T HAPPEN.")
                val blockPalette = schematicV1.blockPalette
                val flatExtraData = schematicV1.flatTagData.map { it.copy() }
                val oldToNewId = ships.associate { Pair(it.second, it.first.id) }
                currentBlockData.updateKeys()

                val offset = MVector3d(
                    ship.chunkClaim.xStart * 16,
                    0,
                    ship.chunkClaim.zStart * 16
                )

                while (currentChunk < currentBlockData.sortedChunkKeys.size) {
                    placeChunk(level, oldToNewId, currentBlockData, blockPalette, flatExtraData, offset)
                    currentChunk++

                    if (getNow_ms() - start > timeout) { return false }
                }

                currentChunk = 0
                currentShip++
                if (getNow_ms() - start > timeout) { return false }
            }
            delayedBlockEntityLoading.forEach { _, _, _, it -> it() }
            afterPasteCallbacks.forEach { it() }
            currentShip = 0
            currentChunk = 0
            while (currentShip < ships.size) {
                val ship = ships[currentShip].first
                val currentBlockData = schematicV1.blockData[ships[currentShip].second] ?: throw RuntimeException("BLOCK DATA IS NULL. SHOULDN'T HAPPEN.")
                currentBlockData.updateKeys()

                val offset = MVector3d(
                    ship.chunkClaim.xStart * 16,
                    0,
                    ship.chunkClaim.zStart * 16
                )

                while (currentChunk < currentBlockData.sortedChunkKeys.size) {
                    updateChunk(level, currentBlockData, offset)
                    currentChunk++

                    if (getNow_ms() - start > timeout) { return false }
                }

                currentChunk = 0
                currentShip++
                if (getNow_ms() - start > timeout) { return false }
            }

            return true
        }
    }

    fun queueShipsSavingEvent(
        level: ServerLevel,
        uuid: UUID,
        ships: List<ServerShip>,
        schematicV1: IShipSchematicDataV1,
        padBoundary: Boolean,
        postPlacementFn: () -> Unit) {
        saveData[uuid] = SchemSaveItem(level, schematicV1, ships, postPlacementFn, padBoundary)
    }

    class SchemSaveItem(
        val level: ServerLevel,
        val schematicV1: IShipSchematicDataV1,
        val ships: List<ServerShip>,
        val postCopyFn: () -> Unit,
        val padBoundary: Boolean
    ) {
        var currentShip = 0
        var currentChunk = 0

        var copyingShip = -1
        var minCx = 0
        var maxCx = 0

        var minCz = 0
        var maxCz = 0

        var cx = 0
        var cz = 0

        private fun saveChunk(level: ServerLevel, chunk: LevelChunk, ships: List<ServerShip>,
                              data: ChunkyBlockData<BlockItem>, flatExtraData: MutableList<CompoundTag>, blockPalette: IBlockStatePalette,
                              chunkMin: BlockPos, cX: Int, cZ: Int,
                              minX: Int, maxX: Int, minZ: Int, maxZ: Int, minY: Int, maxY: Int) {
            for (y in minY until maxY) {
            for (x in minX until maxX) {
            for (z in minZ until maxZ) {
                val cpos = BlockPos(x, y, z)
                val state = chunk.getBlockState(cpos)
                if (state.isAir) {continue}
                val block = state.block

                val bePos = BlockPos(x + cX * 16, y, z + cZ * 16)
                val be = chunk.getBlockEntity(bePos)

                var tag: CompoundTag? = if (block is ICopyableBlock) {block.onCopy(level, cpos, state, be, ships)} else {null}
                val fed = if (be == null) {-1} else {
                    if (tag == null) {tag = be.saveWithFullMetadata()!!}
                    flatExtraData.add(tag)
                    flatExtraData.size - 1
                }

                val cancel = SchemCompatObj.onCopy(level, bePos, state, ships, be, tag)
                if (cancel) {
                    if (fed != -1) { flatExtraData.removeLast() }
                    continue
                }

                val id = blockPalette.toId(state)
                data.add(
                    x + cX * 16 - chunkMin.x,
                    y           - chunkMin.y,
                    z + cZ * 16 - chunkMin.z,
                    BlockItem(id, fed)
                )
            } } }
        }

        fun save(start: Long, timeout: Long): Boolean {
            val blockData = schematicV1.blockData
            val fed = schematicV1.flatTagData
            val blockPalette = schematicV1.blockPalette

            while (currentShip < ships.size) {
                val ship = ships[currentShip]
                val data = blockData.getOrPut(ship.id) { ChunkyBlockData() }

                val boundsAABB = AABBi(ship.shipAABB!!)

                if (padBoundary) {
                    boundsAABB.minX -= 1
                    boundsAABB.minY -= 1
                    boundsAABB.minZ -= 1
                    boundsAABB.maxX += 1
                    boundsAABB.maxY += 1
                    boundsAABB.maxZ += 1
                }

                val chunkMin = BlockPos(
                    ship.chunkClaim.xStart * 16,
                    0,
                    ship.chunkClaim.zStart * 16
                )

                if (copyingShip != currentShip) {
                    minCx = boundsAABB.minX() shr 4
                    maxCx = boundsAABB.maxX() shr 4
                    minCz = boundsAABB.minZ() shr 4
                    maxCz = boundsAABB.maxZ() shr 4

                    cx = minCx
                    cz = minCz

                    copyingShip = currentShip
                }

                val minY = boundsAABB.minY()
                val maxY = boundsAABB.maxY()

                while (cx < maxCx+1) {
                    var minX = 0
                    var maxX = 16

                    if (cx == minCx) {minX = boundsAABB.minX() and 15}
                    if (cx == maxCx) {maxX = boundsAABB.maxX() and 15}

                    while (cz < maxCz+1) {
                        var minZ = 0
                        var maxZ = 16

                        if (cz == minCx) {minZ = boundsAABB.minZ() and 15}
                        if (cz == maxCx) {maxZ = boundsAABB.maxZ() and 15}

                        saveChunk(level, level.getChunk(cx, cz), ships, data, fed, blockPalette, chunkMin, cx, cz, minX, maxX, minZ, maxZ, minY, maxY)
                        cz++
                        if (getNow_ms() - start > timeout) { return false }
                    }
                    cz = minCz
                    cx++
                }

                currentChunk = 0
                currentShip++
            }

            return true
        }
    }

    fun queueShipsUnfreezeEvent(uuid: UUID, ships: List<ServerShip>, time: Int) {
        unfreezeData[uuid] = SchemUnfreezeShips(ships, time)
    }

    data class SchemUnfreezeShips(
        val ships: List<ServerShip>,
        val waitFor: Int,
    ) {
        var time = 0
    }

    init {
        TickEvent.SERVER_POST.register {
            if (placeData.isEmpty()) {return@register}
            val timeout = VMConfig.SERVER.SCHEMATICS.TIMEOUT_TIME.toLong()
            val start = getNow_ms()
            while (getNow_ms() - start < timeout) {
                if (placeLastPosition >= placeLastKeys.size) {
                    placeLastKeys = placeData.keys.toList()
                    placeLastPosition = 0
                    if (placeLastKeys.isEmpty()) {return@register}
                }

                val item = placeData[placeLastKeys[placeLastPosition]]
                val result = try {item?.place(start, timeout)} catch (e: Exception) {ELOG("Failed to place item with exception:\n${e.stackTraceToString()}"); null}
                if (result == null || result) {
                    item!!.postPlacementFn()
                    placeData.remove(placeLastKeys[placeLastPosition])
                }

                placeLastPosition++
            }
        }

        TickEvent.SERVER_POST.register {
            if (saveData.isEmpty()) {return@register}
            val timeout = VMConfig.SERVER.SCHEMATICS.TIMEOUT_TIME.toLong()
            val start = getNow_ms()
            while (getNow_ms() - start < timeout) {
                if (saveLastPosition >= saveLastKeys.size) {
                    saveLastKeys = saveData.keys.toList()
                    saveLastPosition = 0
                    if (saveLastKeys.isEmpty()) {return@register}
                }

                val item = saveData[saveLastKeys[saveLastPosition]]
                val result = try {item?.save(start, timeout)} catch (e: Exception) {ELOG("Failed to copy item with exception:\n${e.stackTraceToString()}"); null}
                if (result == null || result) {
                    item!!.postCopyFn()
                    saveData.remove(saveLastKeys[saveLastPosition])
                }

                saveLastPosition++
            }
        }

        TickEvent.SERVER_POST.register {
            unfreezeData.mapNotNull {(uuid, item) ->
                item.time++
                if (item.time < item.waitFor) {return@mapNotNull null}

                item.ships.forEach { it.isStatic = false }
                uuid
            }.forEach { unfreezeData.remove(it) }
        }
    }

    override fun close() {
        placeData.clear()
        saveData.clear()
    }
}