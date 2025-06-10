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
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.spaceeye.valkyrien_ship_schematics.SchematicRegistry
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockItem
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ChunkyBlockData
import net.spaceeye.valkyrien_ship_schematics.containers.v1.EntityItem
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableBlock
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.compat.schem.SchemCompatObj
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.toolgun.SELOG
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.translate.ONE_OF_THE_SHIPS_IS_TOO_TALL
import net.spaceeye.vmod.translate.SCHEMATIC_HAD_ERROR_DURING_COPYING
import net.spaceeye.vmod.translate.SCHEMATIC_HAD_ERROR_DURING_PLACING
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.joml.primitives.AABBd
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.expand
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.entity.VSPhysicsEntity
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.yRange
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object SchematicActionsQueue: ServerClosable() {
    init {
        //why here? idk
        SchematicRegistry.register(VModShipSchematicV2::class)
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

    fun queueShipsCreationEvent(level: ServerLevel, player: ServerPlayer?, uuid: UUID, ships: List<Pair<() -> ServerShip, Long>>, schematicV1: IShipSchematicDataV1, postPlacementFn: (ships: List<Pair<Long, ServerShip>>, centerPositions: Map<ShipId, Pair<JVector3d, JVector3d>>, entityCreationFn: () -> Unit) -> Unit) {
        placeData[uuid] = SchemPlacementItem(level, player, schematicV1, ships, postPlacementFn)
    }

    private inline fun <T> logThrowables(onError: () -> Unit = {}, fn: () -> T): T? {
        return try { fn() } catch (e: Throwable) {ELOG(e.stackTraceToString()); onError(); null}
    }

    private class SchemPlacementItem(
        val level: ServerLevel,
        val player: ServerPlayer?,
        val schematicV1: IShipSchematicDataV1,
        val shipsToCreate: List<Pair<() -> ServerShip, Long>>,
        val postPlacementFn: (ships: List<Pair<Long, ServerShip>>, centerPositions: Map<ShipId, Pair<JVector3d, JVector3d>>, entityCreationFn: () -> Unit) -> Unit
    ) {
        var currentShip = 0
        var currentChunk = 0

        val oldToNewId = mutableMapOf<Long, ShipId>()
        val centerPositions = mutableMapOf<ShipId, Pair<JVector3d, JVector3d>>()
        var createdShips = mutableListOf<Pair<Long, ServerShip>>()
        var afterPasteCallbacks = mutableListOf<() -> Unit>()
        var delayedBlockEntityLoading = ChunkyBlockData<() -> Unit>()

        var hadNonfatalErrors = 0
        lateinit var entityCreationFn: () -> Unit

        private fun placeChunk(level: ServerLevel, oldToNewId: Map<Long, Long>, currentChunkData: ChunkyBlockData<BlockItem>, blockPalette: IBlockStatePalette, flatTagData: List<CompoundTag>, shipCenter: MVector3d) {
            currentChunkData.chunkForEach(currentChunk) { x, y, z, it -> logThrowables({hadNonfatalErrors++}) {
                val pos = BlockPos(x + shipCenter.x, y + shipCenter.y, z + shipCenter.z)
                val state = blockPalette.fromId(it.paletteId) ?: run {
                    ELOG("State under ID ${it.paletteId} is null.")
                    hadNonfatalErrors++
                    Blocks.AIR.defaultBlockState()
                }
                if (state.isAir) {return@chunkForEach}
                val block = state.block

                level.getChunkAt(pos).also {
                    it.setBlockState(pos, state, false)
                    it.removeBlockEntity(pos)
                }
                if (it.extraDataId != -1) {
                    val tag = flatTagData[it.extraDataId]
                    tag.putInt("x", pos.x)
                    tag.putInt("y", pos.y)
                    tag.putInt("z", pos.z)

                    var callbacks = mutableListOf<((CompoundTag?) -> CompoundTag?)>()
                    val cb = SchemCompatObj.onPaste(level, oldToNewId, centerPositions, tag, pos, state) { delay, fn -> fn?.let{callbacks.add(it)} }

                    delayedBlockEntityLoading.add(pos.x, pos.y, pos.z) {
                        //refreshing block entities as a long time may pass between its creation and fn call
                        val be = if (state.hasBlockEntity()) {
                            val newBe = (state.block as EntityBlock).newBlockEntity(pos, state)
                            newBe?.also{ level.getChunkAt(pos).addAndRegisterBlockEntity(it) }
                        } else null

                        val tag =
                            if (block is ICopyableBlock) {block.onPaste(level, pos, state, oldToNewId, centerPositions, tag)} else {null}
                            ?: callbacks.let {
                                var ret: CompoundTag? = null
                                for (it in it) { ret = it(tag); if (ret != null) {break} }
                                ret
                            } ?: tag

                        be?.load(tag)
                        cb?.let { afterPasteCallbacks.add { it(be) } }
                    }
                }
            } }
        }

        private fun updateChunk(level: ServerLevel, currentChunkData: ChunkyBlockData<BlockItem>, shipCenter: MVector3d) {
            currentChunkData.chunkForEach(currentChunk) { x, y, z, it -> logThrowables({hadNonfatalErrors++}) {
                val pos = BlockPos(x + shipCenter.x, y + shipCenter.y, z + shipCenter.z)
                val state = level.getBlockState(pos)

                level.chunkSource.blockChanged(pos)
                level.setBlocksDirty(pos, state, state)
                level.blockUpdated(pos, state.block)
                level.updateNeighbourForOutputSignal(pos, state.block)
            } }
        }

        private var shipsInfo: Map<Long, IShipInfo>? = null
        var createdAllBlocks = false
        var flatTagDataCopy: List<CompoundTag>? = null

        fun place(start: Long, timeout: Long): Boolean? {
            val shipsInfo = shipsInfo ?: let {
                //should only be called once
                schematicV1.blockData.forEach { (_, data) -> data.updateKeys() }

                val info = schematicV1 as IShipSchematic
                info.info!!.shipsInfo
                    .associate { Pair(it.id, it) }
                    .also { it.forEach {
                        val bounds = it.value.centeredShipAABB
                        if (bounds.maxY() - bounds.minY() > level.yRange.size) {
                            player?.let { ServerToolGunState.sendErrorTo(it, ONE_OF_THE_SHIPS_IS_TOO_TALL) }
                            return null
                    } } }
                    .also { shipsInfo = it }
            }

            while (!createdAllBlocks && currentShip < shipsToCreate.size) {
                if (createdShips.size - 1 < currentShip) {
                    createdShips.add(shipsToCreate[currentShip].let { Pair(it.second, it.first.invoke()) })
                    createdShips.last().also { (shipId, ship) -> //old ship id
                        oldToNewId[shipId] = ship.id
                        val info = shipsInfo[shipId]!!
                        val offset = info.previousCenterPosition.let { it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d()) }
                        centerPositions[shipId] = Pair(
                            info.previousCenterPosition,
                            JVector3d(
                                ship.chunkClaim.xMiddle * 16.0 - 7 - offset.x,
                                level.yRange.center.toDouble()     - offset.y,
                                ship.chunkClaim.zMiddle * 16.0 - 7 - offset.z,
                            )
                        )
                    }
                    ShipSchematic.onPasteBeforeBlocksAreLoaded(level, createdShips.toMap(), createdShips[currentShip], centerPositions, schematicV1.extraData.toMap())
                }
                val ship = createdShips[currentShip].second

                val currentBlockData = schematicV1.blockData[shipsToCreate[currentShip].second] ?: throw RuntimeException("Block data is null")
                val blockPalette = schematicV1.blockPalette
                val flatExtraData = flatTagDataCopy ?: schematicV1.flatTagData
                    .map { it.copy() }
                    .also { flatTagDataCopy = it }

                val shipCenter = MVector3d(
                    ship.chunkClaim.xMiddle * 16 - 7,
                    level.yRange.center,
                    ship.chunkClaim.zMiddle * 16 - 7
                )

                while (currentChunk < currentBlockData.sortedChunkKeys.size) {
                    placeChunk(level, oldToNewId, currentBlockData, blockPalette, flatExtraData, shipCenter)
                    currentChunk++

                    if (getNow_ms() - start > timeout) { return false }
                }

                currentChunk = 0
                currentShip++
            }
            if (!createdAllBlocks) {
                // loading all block entities and ICopyableBlock's
                delayedBlockEntityLoading.forEach { _, _, _, it -> logThrowables({hadNonfatalErrors++}) { it() } }
                afterPasteCallbacks.forEach { logThrowables({hadNonfatalErrors++}) { it() } }
                currentShip = 0
                currentChunk = 0
                createdAllBlocks = true
            }

            //Updating all blocks
            while (currentShip < shipsToCreate.size) {
                val ship = createdShips[currentShip].second
                val currentBlockData = schematicV1.blockData[shipsToCreate[currentShip].second] ?: throw RuntimeException("Block data is null")

                val shipCenter = MVector3d(
                    ship.chunkClaim.xMiddle * 16 - 7,
                    level.yRange.center,
                    ship.chunkClaim.zMiddle * 16 - 7
                )

                while (currentChunk < currentBlockData.sortedChunkKeys.size) {
                    updateChunk(level, currentBlockData, shipCenter)
                    currentChunk++

                    if (getNow_ms() - start > timeout) { return false }
                }

                currentChunk = 0
                currentShip++
            }

            entityCreationFn = {
            schematicV1.entityData.forEach { (oldId, entities) ->
                val newShip = level.shipObjectWorld.allShips.getById(oldToNewId[oldId]!!)!!
                entities.forEach { (pos, tag) -> logThrowables({hadNonfatalErrors++}) {
                    val tag = tag.copy()

                    val info = shipsInfo[oldId]!!
                    val offset = info.previousCenterPosition.let { it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d()) }
                    val shipCenter = Vector3d(
                        newShip.chunkClaim.xMiddle * 16.0 - 7 - offset.x,
                        level.yRange.center.toDouble()        - offset.y,
                        newShip.chunkClaim.zMiddle * 16.0 - 7 - offset.z,
                    )

                    val newPos = pos.add(shipCenter.toJomlVector3d(), org.joml.Vector3d())

                    val posTag = ListTag()
                    posTag.add(DoubleTag.valueOf(newPos.x))
                    posTag.add(DoubleTag.valueOf(newPos.y))
                    posTag.add(DoubleTag.valueOf(newPos.z))

                    tag.put("Pos", posTag)
                    tag.remove("UUID")

                    SchemCompatObj.onEntityPaste(level, oldToNewId, tag, Vector3d(newPos), shipCenter)
                    val entity = EntityType.create(tag, level).get()
                    entity.moveTo(newPos.x, newPos.y, newPos.z)
                    if (entity is Mob) {
                        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos(newPos.toMinecraft())), MobSpawnType.STRUCTURE, null, tag)
                    }
                    level.addFreshEntityWithPassengers(entity)
                } }
            } }

            //TODO think of a way to make str into translatable
            if (hadNonfatalErrors > 0) { player?.let { ServerToolGunState.sendErrorTo(it, "Schematic had $hadNonfatalErrors nonfatal errors") } }

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

    private class SchemSaveItem(
        val level: ServerLevel,
        val player: ServerPlayer?,
        val schematicV1: IShipSchematicDataV1,
        val ships: List<ServerShip>,
        val postCopyFn: () -> Unit,
        val padBoundary: Boolean
    ) {
        var centerPositions: Map<Long, JVector3d>? = null

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
                              shipCenter: BlockPos, cX: Int, cZ: Int,
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

                var tag: CompoundTag? = if (block is ICopyableBlock) {block.onCopy(level, cpos, state, be, ships, centerPositions!!)} else {null}
                if (tag == null) {tag = be?.saveWithFullMetadata()}

                val fed = if (tag == null) {-1} else {
                    flatExtraData.add(tag)
                    flatExtraData.size - 1
                }

                val cancel = SchemCompatObj.onCopy(level, bePos, state, ships, centerPositions!!, be, tag)
                if (cancel) {
                    if (fed != -1) { flatExtraData.removeLast() }
                    continue
                }

                val id = blockPalette.toId(state)
                data.add(
                    x + cX * 16 - shipCenter.x,
                    y           - shipCenter.y,
                    z + cZ * 16 - shipCenter.z,
                    BlockItem(id, fed)
                )
            } } }
        }

        fun save(start: Long, timeout: Long): Boolean {
            val blockData = schematicV1.blockData
            val fed = schematicV1.flatTagData
            val blockPalette = schematicV1.blockPalette

            if (centerPositions == null) {
                centerPositions = ships.associate {
                    val b = it.shipAABB!!
                    Pair(it.id,
                        JVector3d(
                            (b.maxX() - b.minX()) / 2.0 + b.minX(),
                            (b.maxY() - b.minY()) / 2.0 + b.minY(),
                            (b.maxZ() - b.minZ()) / 2.0 + b.minZ()
                        )
                    )
                }
            }

            while (currentShip < ships.size) {
                val ship = ships[currentShip]
                val data = blockData.getOrPut(ship.id) { ChunkyBlockData() }

                val b = AABBi(ship.shipAABB!!)
                if (padBoundary) { b.expand(1, b) }

                val shipCenter = BlockPos(
                    (b.maxX() - b.minX()) / 2 + b.minX(),
                    (b.maxY() - b.minY()) / 2 + b.minY(),
                    (b.maxZ() - b.minZ()) / 2 + b.minZ()
                )

                if (copyingShip != currentShip) {
                    minCx = b.minX() shr 4
                    maxCx = b.maxX() shr 4
                    minCz = b.minZ() shr 4
                    maxCz = b.maxZ() shr 4

                    cx = minCx
                    cz = minCz

                    copyingShip = currentShip
                }

                while (cx < maxCx+1) {
                    var minX = 0
                    var maxX = 16

                    if (cx == minCx) {minX = b.minX() and 15}
                    if (cx == maxCx) {maxX = b.maxX() and 15}

                    while (cz < maxCz+1) {
                        var minZ = 0
                        var maxZ = 16

                        if (cz == minCx) {minZ = b.minZ() and 15}
                        if (cz == maxCx) {maxZ = b.maxZ() and 15}

                        saveChunk(level, level.getChunk(cx, cz), ships, data, fed, blockPalette, shipCenter, cx, cz, minX, maxX, minZ, maxZ, b.minY, b.maxY)
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
            val savedEntities = mutableSetOf<UUID>()
            while (currentShip < ships.size) {
                val ship = ships[currentShip]
                val aabb = ship.shipAABB!!.toAABBd(AABBd())
                val worldEntities = level.getEntitiesOfClass(Entity::class.java, AABB(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ())) { it !is Player }
                schematicV1.entityData[ship.id] = worldEntities.mapNotNull {
                    if (savedEntities.contains(it.uuid)) { return@mapNotNull null }
                    if (it is VSPhysicsEntity) { return@mapNotNull null }
                    val epos = it.position()
                    //idk why but getEntitiesOfClass also gets shipyard entities of other ships
                    if (level.isBlockInShipyard(epos.x, epos.y, epos.z) && level.getShipManagingPos(epos.x, epos.y, epos.z)?.id != ship.id) {return@mapNotNull null}
                    savedEntities.add(it.uuid)
                    val entityPos = it.position()
                    val shipyardPos = if (level.isBlockInShipyard(entityPos)) {entityPos.toJOML()} else {ship.transform.worldToShip.transformPosition(entityPos.toJOML())}

                    val b = ship.shipAABB!!
                    val shipCenter = Vector3d(
                        (b.maxX() - b.minX()) / 2.0 + b.minX(),
                        (b.maxY() - b.minY()) / 2.0 + b.minY(),
                        (b.maxZ() - b.minZ()) / 2.0 + b.minZ(),
                    )

                    val pos = Vector3d(shipyardPos) - Vector3d(shipCenter)

                    EntityItem(pos.toJomlVector3d(), CompoundTag().also {
                        tag -> it.save(tag)
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
                if (result == null) {
                    placeData.remove(placeLastKeys[placeLastPosition])
                } else if (result) {
                    var tick = 0
                    SessionEvents.serverOnTick.on { (server), unsubscribe ->
                        tick++
                        if (tick > 2) {
                            item!!.postPlacementFn(item.createdShips, item.centerPositions, item.entityCreationFn)
                            unsubscribe()
                        }
                    }

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