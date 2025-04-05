package net.spaceeye.vmod.schematic

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.spaceeye.valkyrien_ship_schematics.SchematicRegistry
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockItem
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ChunkyBlockData
import net.spaceeye.valkyrien_ship_schematics.containers.v1.EntityItem
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableBlock
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.compat.schem.SchemCompatObj
import net.spaceeye.vmod.utils.BlockPos
import net.spaceeye.vmod.toolgun.SELOG
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.translate.SCHEMATIC_HAD_ERROR_DURING_COPYING
import net.spaceeye.vmod.translate.SCHEMATIC_HAD_ERROR_DURING_PLACING
import net.spaceeye.vmod.translate.SCHEMATIC_HAD_NONFATAL_ERRORS
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.joml.primitives.AABBd
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.yRange
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

    fun queueShipsCreationEvent(level: ServerLevel, player: ServerPlayer?, uuid: UUID, ships: List<Pair<() -> ServerShip, Long>>, schematicV1: IShipSchematicDataV1, postPlacementFn: (ships: List<Pair<ServerShip, Long>>) -> Unit) {
        placeData[uuid] = SchemPlacementItem(level, player, schematicV1, ships, postPlacementFn)
    }

    class SchemPlacementItem(
        val level: ServerLevel,
        val player: ServerPlayer?,
        val schematicV1: IShipSchematicDataV1,
        val shipsToCreate: List<Pair<() -> ServerShip, Long>>,
        val postPlacementFn: (ships: List<Pair<ServerShip, Long>>) -> Unit
    ) {
        var currentShip = 0
        var currentChunk = 0

        val oldToNewId = mutableMapOf<Long, ShipId>()
        var createdShips = mutableListOf<Pair<ServerShip, Long>>()
        var afterPasteCallbacks = mutableListOf<() -> Unit>()
        var delayedBlockEntityLoading = ChunkyBlockData<() -> Unit>()

        var hadNonfatalErrors = false

        private fun placeChunk(level: ServerLevel, oldToNewId: Map<Long, Long>, currentChunkData: ChunkyBlockData<BlockItem>, blockPalette: IBlockStatePalette, flatTagData: List<CompoundTag>, offset: MVector3d) {
            currentChunkData.chunkForEach(currentChunk) { x, y, z, it ->
                val pos = BlockPos(x + offset.x, y + offset.y, z + offset.z)
                val state = blockPalette.fromId(it.paletteId) ?: run {
                    ELOG("State under ID ${it.paletteId} is null.")
                    hadNonfatalErrors = true
                    Blocks.AIR.defaultBlockState()
                }
                if (state.isAir) {return@chunkForEach}
                val block = state.block

                level.getChunkAt(pos).setBlockState(pos, state, false)
                if (block is ICopyableBlock) block.onPasteNoTag(level, pos, state, oldToNewId)
                if (it.extraDataId != -1) {
                    val tag = flatTagData[it.extraDataId]
                    tag.putInt("x", pos.x)
                    tag.putInt("y", pos.y)
                    tag.putInt("z", pos.z)

                    var delayLoading = true
                    var callback: ((CompoundTag?) -> CompoundTag?)? = null
                    var bcb: ((BlockEntity?) -> Unit)? = null
                    val cb = SchemCompatObj.onPaste(level, oldToNewId, tag, pos, state) { delay, fn -> delayLoading = delay; callback = fn }
                    if (block is ICopyableBlock) block.onPaste(level, pos, state, oldToNewId, tag, {delay, fn -> delayLoading = delay; callback = fn }) { bcb = it }

                    val fn = {
                        val be = level.getChunkAt(pos).getBlockEntity(pos)
                        be?.load(callback?.invoke(tag) ?: tag) ?: run {
                            ELOG("$pos is not a block entity while data says otherwise. It can cause problems.")
                            hadNonfatalErrors = true
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
            while (currentShip < shipsToCreate.size) {
                if (createdShips.size - 1 < currentShip) {
                    createdShips.add(shipsToCreate[currentShip].let { Pair(it.first.invoke(), it.second) })
                    ShipSchematic.onPasteBeforeBlocksAreLoaded(level, createdShips, createdShips[currentShip], schematicV1.extraData.toMap())
                }
                val ship = createdShips[currentShip].first

                val currentBlockData = schematicV1.blockData[shipsToCreate[currentShip].second] ?: throw RuntimeException("Block data is null")
                val blockPalette = schematicV1.blockPalette
                val flatExtraData = schematicV1.flatTagData.map { it.copy() }
                oldToNewId.clear()
                oldToNewId.putAll(createdShips.associate { Pair(it.second, it.first.id) })
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
            while (currentShip < shipsToCreate.size) {
                val ship = createdShips[currentShip].first
                val currentBlockData = schematicV1.blockData[shipsToCreate[currentShip].second] ?: throw RuntimeException("Block data is null")
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

            currentShip = 0
            while (currentShip < shipsToCreate.size) {
                schematicV1.entityData.forEach { (oldId, entities) ->
                    val newShip = level.shipObjectWorld.allShips.getById(oldToNewId[oldId]!!)!!
                    entities.forEach { (pos, tag) ->
                        val tag = tag.copy()

                        val shipCenter = Vector3d(
                            newShip.chunkClaim.xMiddle*16-7,
                            level.yRange.center,
                            newShip.chunkClaim.zMiddle*16-7,
                        )

                        val newPos = pos.add(shipCenter.toJomlVector3d(), org.joml.Vector3d())

                        val posTag = ListTag()
                        posTag.add(DoubleTag.valueOf(newPos.x))
                        posTag.add(DoubleTag.valueOf(newPos.y))
                        posTag.add(DoubleTag.valueOf(newPos.z))

                        tag.put("Pos", posTag)
                        tag.remove("UUID")

                        try {
                            SchemCompatObj.onEntityPaste(level, oldToNewId, tag, Vector3d(newPos), shipCenter)
                            val entity = EntityType.create(tag, level).get()
                            entity.moveTo(newPos.x, newPos.y, newPos.z)
                            if (entity is Mob) {
                                entity.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos(newPos.x.toInt(), newPos.y.toInt(), newPos.z.toInt())), MobSpawnType.STRUCTURE, null, tag)
                            }
                            level.addFreshEntityWithPassengers(entity)
                        } catch (_: Exception) {}
                    }
                }

                currentShip++
            }

            if (hadNonfatalErrors) { player?.let { ServerToolGunState.sendErrorTo(it, SCHEMATIC_HAD_NONFATAL_ERRORS) } }

            return true
        }
    }

    fun queueShipsSavingEvent(
        level: ServerLevel,
        player: ServerPlayer?,
        uuid: UUID,
        ships: List<ServerShip>,
        schematicV1: IShipSchematicDataV1,
        padBoundary: Boolean,
        postPlacementFn: () -> Unit) {
        saveData[uuid] = SchemSaveItem(level, player, schematicV1, ships, postPlacementFn, padBoundary)
    }

    class SchemSaveItem(
        val level: ServerLevel,
        val player: ServerPlayer?,
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
            currentShip = 0
            while (currentShip < ships.size) {
                val ship = ships[currentShip]
                val aabb = ship.shipAABB!!.toAABBd(AABBd())
                val worldEntities = level.getEntitiesOfClass(Entity::class.java, AABB(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ())) { it !is Player }
                schematicV1.entityData[ship.id] = worldEntities.map {
                    val entityPos = it.position()
                    val shipyardPos = if (level.isBlockInShipyard(entityPos)) {entityPos.toJOML()} else {ship.transform.worldToShip.transformPosition(entityPos.toJOML())}

                    val shipCenter = Vector3d(
                        ship.chunkClaim.xMiddle*16-7,
                        level.yRange.center,
                        ship.chunkClaim.zMiddle*16-7,
                    )

                    val pos = Vector3d(shipyardPos) - Vector3d(shipCenter)

                    EntityItem(pos.toJomlVector3d(), CompoundTag().also {
                        tag -> it.save(tag);
                        SchemCompatObj.onEntityCopy(level, it, tag, pos, shipCenter)
                    })
                }

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
                val result = try {item?.place(start, timeout)} catch (e: Exception) { SELOG("Failed to place item with exception:\n${e.stackTraceToString()}", item?.player, SCHEMATIC_HAD_ERROR_DURING_PLACING); null}
                if (result == null || result) {
                    item!!.postPlacementFn(item.createdShips)
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
                val result = try {item?.save(start, timeout)} catch (e: Exception) {SELOG("Failed to copy item with exception:\n${e.stackTraceToString()}", item?.player, SCHEMATIC_HAD_ERROR_DURING_COPYING); null}
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