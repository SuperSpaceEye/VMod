package net.spaceeye.vmod.vEntityManaging

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.valkyrien_ship_schematics.SchematicEventRegistry
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.ILOG
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.vEntityManaging.VEntityTypes.getType
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.utils.PosMapList
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.addCustomServerClosable
import net.spaceeye.vmod.vEntityManaging.legacy.LegacyConstraintFixers
import org.apache.commons.lang3.tuple.MutablePair
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import kotlin.math.max

internal const val SAVE_TAG_NAME_STRING = "vmod_VEntities"
internal const val LEGACY_SAVE_TAG_NAME = "vmod_ships_constraints" //TODO REMOVE EVENTUALLY

typealias VEntityId = Int

fun VEntityId?.addFor(player: Player): VEntityId? {
    ServerToolGunState.playersVEntitiesStack.getOrPut(player.uuid) { mutableListOf() }.add(this ?: return null)
    return this
}

@Internal
class VEntityManager: SavedData() {
    // shipToVEntity and idToVEntity should share VEntity
    internal val shipToVEntity = mutableMapOf<ShipId, MutableList<VEntity>>()
    private val idToVEntity = mutableMapOf<VEntityId, VEntity>()
    private var vEntityIdCounter = 0
    private val idToDisabledCollisions = mutableMapOf<ShipId, MutableMap<ShipId, MutablePair<Int, MutableList<(() -> Unit)?>>>>()

    private val tickingVEntities = Collections.synchronizedList(mutableListOf<Tickable>())

    private val toLoadVEntities = mutableListOf<VEntity>()
    private val groupedToLoadVEntities = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipDataStatus = mutableMapOf<ShipId, ShipData>()

    private val posToMId = PosMapList<VEntityId>()

    fun saveActiveVEntities(tag: CompoundTag): CompoundTag {
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        val vEntitiesTag = ListTag()
        idToVEntity.forEach { (_, it) -> saveVEntityToList(it, dimensionIds, vEntitiesTag) }
        tag.put(SAVE_TAG_NAME_STRING, vEntitiesTag)

        return tag
    }

    fun loadVEntityFromTag(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity? {
        var strType = "Unknown"

        try {
        strType = tag.getString("VEntityType")
        val data = tag.getCompound("data")
        return VEntityTypes
            .strTypeToSupplier(strType)
            .get()
            .nbtDeserialize(data, lastDimensionIds) ?: run { ELOG("Failed to deserialize VEntity of type $strType"); null }
        } catch (e: Exception) { ELOG("Failed to load VEntity of type $strType\n${e.stackTraceToString()}")
        } catch (e: Error    ) { ELOG("Failed to load VEntity of type $strType\n${e.stackTraceToString()}")}

        return null
    }

    internal fun saveVEntityToList(
        vEntity: VEntity,
        dimensionIds: Collection<ShipId>,
        vEntitiesTag: ListTag
    ) {
        try {
            if (!vEntity.stillExists(allShips!!, dimensionIds)) { return }

            val data = vEntity.nbtSerialize() ?: run { WLOG("Unable to serialize VEntity ${vEntity.getType()} with ID ${vEntity.mID}"); null } ?: return
            val ctag = CompoundTag()
            ctag.put("data", data)
            ctag.putString("VEntityType", vEntity.getType())
            vEntitiesTag.add(ctag)
        } catch (e: Exception) { ELOG("Failed to save VEntity of type ${vEntity.getType()}\n${e.stackTraceToString()}")
        } catch (e: Error    ) { ELOG("Failed to save VEntity of type ${vEntity.getType()}\n${e.stackTraceToString()}")}
    }

    private fun saveNotLoadedVEntities(tag: CompoundTag): CompoundTag {
        val vEntitiesTag = tag[SAVE_TAG_NAME_STRING] as ListTag
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        val visited = mutableSetOf<LoadingGroup>()
        val visitedVEntities = mutableSetOf<Int>()
        for ((_, groups) in groupedToLoadVEntities) {
            for (group in groups) {
                if (visited.contains(group)) {continue}
                visited.add(group)
                for (vEntity in group.vEntitiesToLoad) {
                    if (visitedVEntities.contains(vEntity.mID)) {continue}
                    visitedVEntities.add(vEntity.mID)
                    saveVEntityToList(vEntity, dimensionIds, vEntitiesTag)

                    if (!vEntity.stillExists(allShips!!, dimensionIds)) {continue}
                }
            }
        }

        return tag
    }

    internal fun saveDimensionIds(tag: CompoundTag): CompoundTag {
        val ids = dimensionToGroundBodyIdImmutable!!

        val idsTag = CompoundTag()
        for ((dimensionId, shipId) in ids) { idsTag.putLong(dimensionId, shipId) }

        tag.put("lastDimensionIds", idsTag)

        return tag
    }

    override fun save(tag: CompoundTag): CompoundTag {
        var tag = saveDimensionIds(tag)
        tag = saveActiveVEntities(tag)
        tag = saveNotLoadedVEntities(tag)

        instance = null
        return tag
    }

    //It's loading them the other way around because it needs to get dimensionId from saved shipId
    internal fun loadDimensionIds(tag: CompoundTag): Map<Long, String> {
        val ret = mutableMapOf<Long, String>()

        if (!tag.contains("lastDimensionIds")) {
            return dimensionToGroundBodyIdImmutable!!.map { (k, v) -> Pair(v, k) }.toMap()
        }

        val dtag = tag["lastDimensionIds"] as CompoundTag
        for (dimensionId in dtag.allKeys) { ret[dtag.getLong(dimensionId)] = dimensionId }

        return ret
    }

    private fun loadDataFromTag(tag: CompoundTag) {
        val lastDimensionIds = loadDimensionIds(tag)
        val vEntitiesTag = (tag[SAVE_TAG_NAME_STRING] ?: return) as ListTag

        var count = 0
        var maxId = vEntityIdCounter
        for (ctag in vEntitiesTag) {
            toLoadVEntities.add(loadVEntityFromTag(ctag as CompoundTag, lastDimensionIds) ?: continue)
            count++
            maxId = max(maxId, toLoadVEntities.last().mID)
        }
        vEntityIdCounter = maxId + 1
        ILOG("Deserialized $count VEntities")
    }

    private fun groupLoadedData() {
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values
        val levels = ServerLevelHolder.server!!.allLevels.associate { Pair(it.dimensionId, it) }

        val groups = mutableMapOf<MutableSet<Long>, MutableList<VEntity>>()

        for (vEntity in toLoadVEntities) {
            if (!vEntity.stillExists(allShips!!, dimensionIds)) { continue }
            val neededIds = vEntity.attachedToShips(dimensionIds).toMutableSet()
            groups.getOrPut(neededIds) { mutableListOf() }.add(vEntity)
        }

        for ((neededIds, toLoad) in groups) {
            val group = LoadingGroup(levels[toLoad[0].dimensionId]!!, toLoad, neededIds, shipDataStatus)
            for (id in neededIds) {
                groupedToLoadVEntities.computeIfAbsent(id) { mutableListOf() }.add(group)
            }
        }
        toLoadVEntities.clear()
    }

    private fun setLoadedId(ship: ServerShip) {
        if (!groupedToLoadVEntities.containsKey(ship.id)) {return}

        //groups are shared between groupedToLoadVEntities, so just notify all groups belonging to this id and just delete all of them
        for (group in groupedToLoadVEntities[ship.id]!!) {
            group.setLoadedId(ship)
        }

        groupedToLoadVEntities.remove(ship.id)
    }

    private fun createVEntities() {
        VSEvents.shipLoadEvent.on { (ship), handler ->
            setLoadedId(ship)
            if (groupedToLoadVEntities.isEmpty()) {
                handler.unregister()
            }
        }
    }

    fun load(tag: CompoundTag) {
        loadDataFromTag(tag)
        groupLoadedData()
        createVEntities()
    }

    private fun tryMakeVEntity(entity: VEntity, level: ServerLevel, onFailure: () -> Unit, rest: () -> Unit) {
        for (i in 0 until 10) {
            if (entity.onMakeVEntity(level)) {return rest()}
        }

        var attempts = 0
        val maxAttempts = VMConfig.SERVER.CONSTRAINT_CREATION_ATTEMPTS
        RandomEvents.serverOnTick.on { _, unsubscribe ->
            if (attempts > maxAttempts) {
                onFailure()
                return@on unsubscribe()
            }

            if (entity.onMakeVEntity(level)) {
                rest()
                return@on unsubscribe()
            }

            attempts++
        }
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    fun makeVEntity(level: ServerLevel, entity: VEntity, callback: ((VEntityId?) -> Unit)) {
        entity.mID = (vEntityIdCounter++)

        tryMakeVEntity(entity, level, {
            ELOG("Was not able to create VEntity of type ${entity.getType()} under ID ${entity.mID}")
            callback(null)
        }) {
            entity.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipToVEntity.computeIfAbsent(it) { mutableListOf() }.add(entity) }
            idToVEntity[entity.mID] = entity
            if (entity is Tickable) { tickingVEntities.add(entity) }
            entity.getAttachmentPositions().forEach { posToMId.addItemTo(entity.mID, it) }

            setDirty()

            callback(entity.mID)
        }
    }

    fun getVEntity(id: VEntityId): VEntity? = idToVEntity[id]

    fun removeVEntity(level: ServerLevel, id: VEntityId): Boolean {
        val entity = idToVEntity[id] ?: return false

        entity.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { (shipToVEntity[it] ?: return@forEach).remove(entity) }
        entity.onDeleteVEntity(level)
        idToVEntity.remove(id)
        if (entity is Tickable) { tickingVEntities.remove(entity) }
        entity.getAttachmentPositions().forEach { posToMId.removeItemFromPos(entity.mID, it) }

        setDirty()
        return true
    }

    fun getAllVEntitiesIdOfId(shipId: ShipId): List<VEntityId> {
        val ventities = shipToVEntity[shipId] ?: return listOf()
        return ventities.map { it.mID }
    }

    @Internal
    fun makeVEntityWithId(level: ServerLevel, entity: VEntity, id: Int, callback: ((VEntityId?) -> Unit)) {
        if (id == -1) { throw AssertionError("makeVEntityWithId was called without a specific id") }

        entity.mID = id
        tryMakeVEntity(entity, level, {
            ELOG("Was not able to create VEntity of type ${entity.getType()} under ID ${entity.mID}")
            callback(null)
        }) {
            entity.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipToVEntity.computeIfAbsent(it) { mutableListOf() }.add(entity) }
            if (idToVEntity.contains(entity.mID)) { ELOG("OVERWRITING AN ALREADY EXISTING VEntity IN makeVEntityWithId. SOMETHING PROBABLY WENT WRONG AS THIS SHOULDN'T HAPPEN.") }
            idToVEntity[entity.mID] = entity
            if (entity is Tickable) { tickingVEntities.add(entity) }
            entity.getAttachmentPositions().forEach { posToMId.addItemTo(entity.mID, it) }

            setDirty()
            callback(entity.mID)
        }
    }

    fun tryGetIdsOfPosition(pos: BlockPos): List<VEntityId>? {
        return posToMId.getItemsAt(pos)
    }

    fun disableCollisionBetween(level: ServerLevel, shipId1: ShipId, shipId2: ShipId, callback: (() -> Unit)? = null): Boolean {
        if (!level.shipObjectWorld.disableCollisionBetweenBodies(shipId1, shipId2)) { return false }

        idToDisabledCollisions.getOrPut(shipId1) { mutableMapOf() }.compute (shipId2) { _, pair-> if (pair == null) { MutablePair(1, mutableListOf(callback)) } else { pair.left++; pair.right.add(callback); pair } }
        idToDisabledCollisions.getOrPut(shipId2) { mutableMapOf() }.compute (shipId1) { _, pair-> if (pair == null) { MutablePair(1, mutableListOf(callback)) } else { pair.left++; pair.right.add(callback); pair } }
        return true
    }

    fun enableCollisionBetween(level: ServerLevel, shipId1: ShipId, shipId2: ShipId) {
        val map = idToDisabledCollisions[shipId1]
        if (map == null) {level.shipObjectWorld.enableCollisionBetweenBodies(shipId1, shipId2); return}
        val value = map[shipId2]
        if (value == null) {level.shipObjectWorld.enableCollisionBetweenBodies(shipId1, shipId2); return}
        value.left--
        if (value.left > 0) {
            idToDisabledCollisions[shipId2]!!.compute(shipId1) {_, pair -> pair!!.left--; pair}
            return
        }

        map.remove(shipId2)
        idToDisabledCollisions[shipId2]!!.remove(shipId1)

        if (map.isEmpty()) { idToDisabledCollisions.remove(shipId1) }
        if (idToDisabledCollisions[shipId2]!!.isEmpty()) { idToDisabledCollisions.remove(shipId2) }

        level.shipObjectWorld.enableCollisionBetweenBodies(shipId1, shipId2)

        value.right?.filterNotNull()?.forEach { it.invoke() }
    }

    fun getAllDisabledCollisionsOfId(shipId: ShipId): Map<ShipId, Int>? {
        return idToDisabledCollisions[shipId]?.map { (k, v) -> Pair(k, v.left) }?.toMap()
    }

    //TODO redo when ship splitting actually happens
    private fun shipWasSplitEvent(
        originalShip: ServerShip,
        newShip: ServerShip,
        centerBlock: BlockPos,
        blocks: DenseBlockPosSet) {
        val constraints = getAllVEntitiesIdOfId(originalShip.id)
        if (constraints.isEmpty()) { return }

        val shipChunkX = newShip.chunkClaim.xMiddle
        val shipChunkZ = newShip.chunkClaim.zMiddle

        val worldChunkX = centerBlock.x shr 4
        val worldChunkZ = centerBlock.z shr 4

        val deltaX = worldChunkX - shipChunkX
        val deltaZ = worldChunkZ - shipChunkZ

        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        blocks.forEachChunk { chunkX, chunkY, chunkZ, chunk ->
            val sourceChunk = level!!.getChunk(chunkX, chunkZ)
            val destChunk = level!!.getChunk(chunkX - deltaX, chunkZ - deltaZ)

            chunk.forEach { x, y, z ->
                val fromPos = BlockPos((sourceChunk.pos.x shl 4) + x, (chunkY shl 4) + y, (sourceChunk.pos.z shl 4) + z)
                val toPos = BlockPos((destChunk.pos.x shl 4) + x, (chunkY shl 4) + y, (destChunk.pos.z shl 4) + z)

                for (id in posToMId.getItemsAt(fromPos) ?: return@forEach) {
                    val constraint = getVEntity(id) ?: continue

                    constraint.attachedToShips(dimensionIds).forEach { shipToVEntity[it]?.remove(constraint) }
                    constraint.moveShipyardPosition(level!!, fromPos, toPos, newShip.id)
                    constraint.attachedToShips(dimensionIds).forEach { shipToVEntity.getOrPut(it) { mutableListOf() }.add(constraint) }

                    setDirty()
                }
            }
        }
    }

    // TODO REMOVE EVENTUALLY
    // ==============================================================================================

    fun tryLoadLegacyMConstraints(tag: CompoundTag) {
        val lastDimensionIds = loadDimensionIds(tag)
        val newDimensionIds = ServerLevelHolder.shipObjectWorld!!.dimensionToGroundBodyIdImmutable
        val oldToNew = lastDimensionIds.map { Pair(it.key, newDimensionIds[it.value]!!) }.toMap()

        val shipsTag = tag[LEGACY_SAVE_TAG_NAME]!! as CompoundTag

        var count = 0
        var maxId = -1
        for (shipId in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[shipId]!! as ListTag
            val constraints = mutableListOf<VEntity>()
            var strType = "None"
            for (ctag in shipConstraintsTag) {
                try {
                    ctag as CompoundTag
                    strType = ctag.getString("MConstraintType")

                    val vEntity = LegacyConstraintFixers.tryUpdateMConstraint(strType, ctag, oldToNew)

                    maxId = max(maxId, vEntity.mID)
                    constraints.add(vEntity)
                    count++
                } catch (e: Exception) { ELOG("Failed to update constraint of type $strType\n${e.stackTraceToString()}")
                } catch (e: Error    ) { ELOG("Failed to update constraint of type $strType\n${e.stackTraceToString()}")}
            }
            toLoadVEntities.addAll(constraints)
        }
        vEntityIdCounter = maxId + 1
        WLOG("Updated $count constraints")
    }
    // ==============================================================================================

    companion object {
        private var instance: VEntityManager? = null
        private var level: ServerLevel? = null

        // SavedData is saved after VSPhysicsPipelineStage is deleted, so getting allShips and
        // dimensionToGroundBodyIdImmutable from level.shipObjectWorld is impossible, unless you get it's reference
        // before it got deleted
        var dimensionToGroundBodyIdImmutable: Map<DimensionId, ShipId>? = null
        var allShips: QueryableShipData<Ship>? = null

        init {
            makeServerEvents()
            addCustomServerClosable { close() }
            SchematicEventRegistry.register(VModVEntityManagerCopyPasteEvents::class)
        }

        fun setDirty() {
            if (instance == null) {return}
            if (VM.serverStopping) {
                return
            }
            instance!!.setDirty()
        }

        fun close() {
            instance = null
            level = null
            dimensionToGroundBodyIdImmutable = null
            allShips = null
        }

        fun getInstance(): VEntityManager {
            if (instance != null) {return instance!!}
            level = ServerLevelHolder.overworldServerLevel!!

            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun initNewInstance(): VEntityManager {
            level = ServerLevelHolder.overworldServerLevel!!

            dimensionToGroundBodyIdImmutable = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable
            allShips = level!!.shipObjectWorld.allShips

            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun create(): VEntityManager {
            return VEntityManager()
        }

        fun load(tag: CompoundTag): VEntityManager {
            val data = create()

            if (tag.contains(LEGACY_SAVE_TAG_NAME)) {
                data.tryLoadLegacyMConstraints(tag)
            }

            if (tag.contains(SAVE_TAG_NAME_STRING) || tag.contains(LEGACY_SAVE_TAG_NAME)) {
                data.load(tag)
            }

            return data
        }

        private fun makeServerEvents() {
            AVSEvents.serverShipRemoveEvent.on {
                (ship), handler ->
                if (level == null) { return@on }
                val instance = getInstance()
                instance.getAllVEntitiesIdOfId(ship.id).forEach {
                    instance.removeVEntity(level!!, it)
                }
                instance.setDirty()
            }

            TickEvent.SERVER_PRE.register {
                server ->
                getInstance()
                if (instance!!.tickingVEntities.isEmpty()) {return@register}

                val toRemove = mutableListOf<Tickable>()
                instance!!.tickingVEntities.forEach {
                    it.tick(server) {toRemove.add(it)}
                }
                instance!!.tickingVEntities.removeAll(toRemove)
                setDirty()
            }

            AVSEvents.splitShip.on {
                it, handler ->
                instance?.shipWasSplitEvent(it.originalShip, it.newShip, it.centerBlock, it.blocks)
            }
        }
    }
}