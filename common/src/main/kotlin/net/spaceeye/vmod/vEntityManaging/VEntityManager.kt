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
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.utils.PosMapList
import net.spaceeye.vmod.utils.ServerObjectsHolder
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple3
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.addCustomServerClosable
import net.spaceeye.vmod.utils.vs.gtpa
import org.apache.commons.lang3.tuple.MutablePair
import org.jetbrains.annotations.ApiStatus.Internal
import org.joml.Vector3ic
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.apigame.world.PhysLevelCore
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

internal const val SAVE_TAG_NAME_STRING = "vmod_VEntities"

typealias VEntityId = Int

fun VEntityId?.addFor(player: Player): VEntityId? {
    ServerToolGunState.playersVEntitiesStack.getOrPut(player.uuid) { mutableListOf() }.add(this ?: return null)
    return this
}

@Internal
open class VEntityManager: SavedData() {
    // shipToVEntity and idToVEntity should share VEntity
    internal val shipToVEntity = mutableMapOf<ShipId, MutableList<VEntity>>()
    private val idToVEntity = mutableMapOf<VEntityId, VEntity>()
    private var vEntityIdCounter = 0
    private val idToDisabledCollisions = mutableMapOf<ShipId, MutableMap<ShipId, MutablePair<Int, MutableList<(() -> Unit)?>>>>()

    private val tickingVEntities = Collections.synchronizedList(mutableListOf<Tickable>())

    // dimension, ventity, add = true / delete = false
    private val physThreadChanges = ConcurrentLinkedQueue<Tuple3<DimensionId, VEntity, Boolean>>()
    private val physThreadTickable = mutableMapOf<String, MutableMap<Int, Tickable>>()

    private val toLoadVEntities = mutableListOf<VEntity>()
    private val groupedToLoadVEntities = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipDataStatus = mutableMapOf<ShipId, ShipData>()

    private val posToMId: MutableMap<String, PosMapList<VEntityId>> = mutableMapOf()

    fun saveActiveVEntities(tag: CompoundTag): CompoundTag {
        val vEntitiesTag = ListTag()
        idToVEntity.forEach { (_, it) -> saveVEntityToList(it, vEntitiesTag) }
        tag.put(SAVE_TAG_NAME_STRING, vEntitiesTag)

        return tag
    }

    fun loadVEntityFromTag(tag: CompoundTag): VEntity? {
        var strType = "Unknown"

        try {
        strType = tag.getString("VEntityType")
        val data = tag.getCompound("data")
        return VEntityTypes
            .strTypeToSupplier(strType)
            .get()
            .nbtDeserialize(data) ?: run { ELOG("Failed to deserialize VEntity of type $strType"); null }
        } catch (e: Exception) { ELOG("Failed to load VEntity of type $strType\n${e.stackTraceToString()}")
        } catch (e: Error    ) { ELOG("Failed to load VEntity of type $strType\n${e.stackTraceToString()}")}

        return null
    }

    internal fun saveVEntityToList(
        vEntity: VEntity,
        vEntitiesTag: ListTag
    ) {
        try {
            if (!vEntity.stillExists(allShips!!)) { return }

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

        val visited = mutableSetOf<LoadingGroup>()
        val visitedVEntities = mutableSetOf<Int>()
        for ((_, groups) in groupedToLoadVEntities) {
            for (group in groups) {
                if (visited.contains(group)) {continue}
                visited.add(group)
                for (vEntity in group.vEntitiesToLoad) {
                    if (visitedVEntities.contains(vEntity.mID)) {continue}
                    visitedVEntities.add(vEntity.mID)
                    saveVEntityToList(vEntity, vEntitiesTag)

                    if (!vEntity.stillExists(allShips!!)) {continue}
                }
            }
        }

        return tag
    }

    override fun save(tag: CompoundTag): CompoundTag {
        var tag = saveActiveVEntities(tag)
        tag = saveNotLoadedVEntities(tag)

        instance = null
        return tag
    }

    private fun loadDataFromTag(tag: CompoundTag) {
        val vEntitiesTag = (tag[SAVE_TAG_NAME_STRING] ?: return) as ListTag

        var count = 0
        var maxId = vEntityIdCounter
        for (ctag in vEntitiesTag) {
            toLoadVEntities.add(loadVEntityFromTag(ctag as CompoundTag) ?: continue)
            count++
            maxId = max(maxId, toLoadVEntities.last().mID)
        }
        vEntityIdCounter = maxId + 1
        ILOG("Deserialized $count VEntities")
    }

    private fun groupLoadedData() {
        val levels = ServerObjectsHolder.server!!.allLevels.associate { Pair(it.dimensionId, it) }

        val groups = mutableMapOf<MutableSet<Long>, MutableList<VEntity>>()

        for (vEntity in toLoadVEntities) {
            if (!vEntity.stillExists(allShips!!)) { continue }
            val neededIds = vEntity.attachedToShips().toMutableSet()
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
        vsApi.shipLoadEvent.on { event, handler -> val ship = event.ship
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
        var futures = entity.onMakeVEntity(level)

        futures.forEach { it.thenAccept {
            if (!futures.all { it.isDone }) {return@thenAccept}
            if (futures.all { it.getNow(false) }) {
                rest()
                return@thenAccept
            }

            var attempts = 0
            val maxAttempts = VMConfig.SERVER.CONSTRAINT_CREATION_ATTEMPTS

            SessionEvents.serverOnTick.on { _, unsubscribe ->
                if (!futures.all { it.isDone }) {return@on}
                if (futures.all { it.getNow(false) }) {
                    rest()
                    unsubscribe()
                    return@on
                }

                if (attempts > maxAttempts) {
                    onFailure()
                    return@on unsubscribe()
                }

                futures = entity.onMakeVEntity(level)

                attempts++
            }
        } }
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    fun makeVEntity(level: ServerLevel, entity: VEntity, callback: ((VEntityId?) -> Unit)) {
        entity.mID = (vEntityIdCounter++)

        tryMakeVEntity(entity, level, {
            ELOG("Was not able to create VEntity of type ${entity.getType()} under ID ${entity.mID}")
            callback(null)
        }) {
            entity.attachedToShips().forEach { shipToVEntity.computeIfAbsent(it) { mutableListOf() }.add(entity) }
            idToVEntity[entity.mID] = entity
            if (entity is Tickable) {
                tickingVEntities.add(entity)
                physThreadChanges.add(Tuple.of(entity.dimensionId!!, entity, true))
            }
            entity.getAttachmentPoints().forEach { posToMId.getOrPut(entity.dimensionId!!) { PosMapList() }.addItemTo(entity.mID, it.toBlockPos()) }

            setDirty()

            callback(entity.mID)
        }
    }

    fun getVEntity(id: VEntityId): VEntity? = idToVEntity[id]

    fun removeVEntity(level: ServerLevel, id: VEntityId): Boolean {
        val entity = idToVEntity[id] ?: return false

        entity.attachedToShips().forEach { (shipToVEntity[it] ?: return@forEach).remove(entity) }
        entity.onDeleteVEntity(level)
        idToVEntity.remove(id)
        if (entity is Tickable) {
            tickingVEntities.remove(entity)
            physThreadChanges.add(Tuple.of(entity.dimensionId!!, entity, false))
        }
        entity.getAttachmentPoints().forEach { posToMId.getOrPut(entity.dimensionId!!) { PosMapList() }.removeItemFromPos(entity.mID, it.toBlockPos()) }

        setDirty()
        return true
    }

    fun getAllVEntitiesIdOfId(shipId: ShipId): List<VEntityId> = shipToVEntity[shipId]?.map { it.mID } ?: emptyList()

    fun idHasVEntities(shipId: ShipId): Boolean = shipToVEntity[shipId]?.isNotEmpty() == true

    @Internal
    fun makeVEntityWithId(level: ServerLevel, entity: VEntity, id: Int, callback: ((VEntityId?) -> Unit)) {
        if (id == -1) { throw AssertionError("makeVEntityWithId was called without a specific id") }

        entity.mID = id
        tryMakeVEntity(entity, level, {
            ELOG("Was not able to create VEntity of type ${entity.getType()} under ID ${entity.mID}")
            callback(null)
        }) {
            entity.attachedToShips().forEach { shipToVEntity.computeIfAbsent(it) { mutableListOf() }.add(entity) }
            if (idToVEntity.contains(entity.mID)) { ELOG("OVERWRITING AN ALREADY EXISTING VEntity IN makeVEntityWithId. SOMETHING PROBABLY WENT WRONG AS THIS SHOULDN'T HAPPEN.") }
            idToVEntity[entity.mID] = entity
            if (entity is Tickable) {
                tickingVEntities.add(entity)
                physThreadChanges.add(Tuple.of(entity.dimensionId!!, entity, true))
            }
            entity.getAttachmentPoints().forEach { posToMId.getOrPut(entity.dimensionId!!) { PosMapList() }.addItemTo(entity.mID, it.toBlockPos()) }

            setDirty()
            callback(entity.mID)
        }
    }

    fun tryGetIdsOfPosition(dimensionId: DimensionId, pos: BlockPos): List<VEntityId>? = posToMId.getOrPut(dimensionId) { PosMapList() }.getItemsAt(pos)

    fun disableCollisionBetween(level: ServerLevel, shipId1: ShipId, shipId2: ShipId, callback: (() -> Unit)? = null): CompletableFuture<Boolean> {
        idToDisabledCollisions.getOrPut(shipId1) { mutableMapOf() }.compute (shipId2) { _, pair-> if (pair == null) { MutablePair(1, mutableListOf(callback)) } else { pair.left++; pair.right.add(callback); pair } }
        idToDisabledCollisions.getOrPut(shipId2) { mutableMapOf() }.compute (shipId1) { _, pair-> if (pair == null) { MutablePair(1, mutableListOf(callback)) } else { pair.left++; pair.right.add(callback); pair } }
        return level.gtpa.disableCollisionBetweenBodies(shipId1, shipId2)
    }

    fun enableCollisionBetween(level: ServerLevel, shipId1: ShipId, shipId2: ShipId) {
        val map = idToDisabledCollisions[shipId1]
        if (map == null) {level.gtpa.enableCollisionBetweenBodies(shipId1, shipId2); return}
        val value = map[shipId2]
        if (value == null) {level.gtpa.enableCollisionBetweenBodies(shipId1, shipId2); return}
        value.left--
        if (value.left > 0) {
            idToDisabledCollisions[shipId2]!!.compute(shipId1) {_, pair -> pair!!.left--; pair}
            return
        }

        map.remove(shipId2)
        idToDisabledCollisions[shipId2]!!.remove(shipId1)

        if (map.isEmpty()) { idToDisabledCollisions.remove(shipId1) }
        if (idToDisabledCollisions[shipId2]!!.isEmpty()) { idToDisabledCollisions.remove(shipId2) }

        level.gtpa.enableCollisionBetweenBodies(shipId1, shipId2)

        value.right?.filterNotNull()?.forEach { it.invoke() }
    }

    fun getAllDisabledCollisionsOfId(shipId: ShipId): Map<ShipId, Int>? {
        return idToDisabledCollisions[shipId]?.map { (k, v) -> Pair(k, v.left) }?.toMap()
    }

    //TODO optimize?
    fun onBlocksMove(level: ServerLevel, oldShip: ServerShip?, newShip: ServerShip?, oldCenter: Vector3ic, newCenter: Vector3ic, blocks: List<BlockPos>) {
        val hasVEntities = if (oldShip != null) { idHasVEntities(oldShip.id) } else { true }
        if (!hasVEntities) {return}
        val posToMId = posToMId.getOrPut(level.dimensionId) { PosMapList() }

        val oldId = oldShip?.id ?: -1L
        val newId = newShip?.id ?: -1L

        val veToData = mutableMapOf<Int, Pair<VEntity, MutableMap<BlockPos, MutableList<Vector3d>>>>()

        //TODO for this part convert bpos to set, and check result of getAttachmentPoints against set
        val visited = blocks.toMutableSet()
        blocks.forEach { bpos ->
            val vIds = posToMId.getItemsAt(bpos) ?: return@forEach
            vIds.forEach { id ->
                val ve = getVEntity(id) ?: return@forEach
                val belong = ve.getAttachmentPoints(oldId).filter { it.toBlockPos() == bpos }
                veToData.getOrPut(id) { Pair(ve, mutableMapOf()) }.second.getOrPut(bpos) { mutableListOf() }.addAll(belong)
            }
        }

        //TODO maybe get points, convert to bpos, and check all in radius against set instead?
        blocks.forEach { bPos ->
            val mPos = Vector3d(bPos) + 0.5 // moved position
            val box = AABBd((mPos - 0.501).toJomlVector3d(), (mPos + 0.501).toJomlVector3d())
            for (y in -1 .. 1)
            for (x in -1 .. 1)
            for (z in -1 .. 1) {
                val bpos = bPos.offset(x, y, z)
                if (visited.contains(bpos)) continue
                visited.add(bpos)
                val vs = posToMId.getItemsAt(bpos) ?: continue
                if (vs.isEmpty()) continue

                vs.forEach { id ->
                    val ve = getVEntity(id) ?: return@forEach
                    val inside = ve.getAttachmentPoints(oldId).filter { box.containsPoint(it.x, it.y, it.z) }
                    if (inside.isEmpty()) {return@forEach}

                    veToData.getOrPut(id) { Pair(ve, mutableMapOf()) }.second.getOrPut(bpos) { mutableListOf() }.addAll(inside)
                }
            }
        }

        veToData.forEach { id, pair ->
            val (vEntity, data) = pair
            val pointsToMove = data.map { (_, positions) -> positions }.flatten()

            vEntity.attachedToShips().forEach { shipToVEntity[it]?.remove(vEntity) }
            vEntity.getAttachmentPoints(oldId).forEach { posToMId.removeItemFromPos(id, it.toBlockPos()) }

            val res = vEntity.moveAttachmentPoints(level, pointsToMove, oldId, newId, Vector3d(oldCenter), Vector3d(newCenter))

            if (res) {
                vEntity.attachedToShips().forEach { shipToVEntity.getOrPut(it) { mutableListOf() }.add(vEntity) }
                vEntity.getAttachmentPoints(newId).forEach { posToMId.addItemTo(id, it.toBlockPos()) }
            }
        }

        setDirty()
    }

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
            level = ServerObjectsHolder.overworldServerLevel!!

            instance = ServerObjectsHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun initNewInstance(): VEntityManager {
            level = ServerObjectsHolder.overworldServerLevel!!

            dimensionToGroundBodyIdImmutable = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable
            allShips = level!!.shipObjectWorld.allShips

            instance = ServerObjectsHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun create(): VEntityManager {
            return VEntityManager()
        }

        fun load(tag: CompoundTag): VEntityManager {
            val data = create()

            if (tag.contains(SAVE_TAG_NAME_STRING)) {
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

                val unloadedShips = mutableSetOf<ShipId>()
                val loadedShips = mutableSetOf<ShipId>()

                val toUnload = mutableListOf<VEntity>()
                val ticking = instance!!.tickingVEntities.filter { ve ->
                    if (ve.alwaysTick) return@filter true

                    ve as VEntity
                    val shipIds = ve.attachedToShips()
                    if (shipIds.any {unloadedShips.contains(it)}) {return@filter false}

                    shipIds.forEach {
                        if (loadedShips.contains(it)) {return@forEach}

                        val ship = level!!.shipObjectWorld.allShips.getById(it) ?: return@forEach
                        val b = ship.shipAABB ?: return@forEach
                        val pos = BlockPos(
                            ((b.maxX() - b.minX()) / 2.0 + b.minX()).toInt(),
                            ((b.maxY() - b.minY()) / 2.0 + b.minY()).toInt(),
                            ((b.maxZ() - b.minZ()) / 2.0 + b.minZ()).toInt(),
                        )

                        if (level!!.isLoaded(pos)) {
                            loadedShips.add(it)
                            return@forEach
                        }
                        unloadedShips.add(it)
                        toUnload.add(ve)
                        return@filter false
                    }

                    true
                }

                val toRemove = mutableListOf<Tickable>()
                ticking.forEach { it.serverTick(server) { toRemove.add(it) } }
                instance!!.tickingVEntities.removeAll(toRemove)
                instance!!.physThreadChanges.addAll(toRemove.map { it as VEntity; Tuple.of(it.dimensionId!!, it, false) })
                instance!!.physThreadChanges.addAll(toUnload.map { Tuple.of(it.dimensionId!!, it, false) })
                instance!!.physThreadChanges.addAll(ticking .map { it as VEntity; Tuple.of(it.dimensionId!!, it, true) })
                setDirty()
            }

            vsApi.physTickEvent.on { event ->
                val instance = instance ?: return@on
                instance.physThreadChanges.pollUntilEmpty { (dimension, ventity, add) -> ventity as Tickable
                    val map = instance.physThreadTickable.getOrPut(dimension) { mutableMapOf() }
                    if (add) { map[ventity.mID] = ventity } else { map.remove(ventity.mID) }
                }
                instance.physThreadTickable[event.world.dimension]?.forEach {k, ve -> ve.physTick(event.world as PhysLevelCore, event.delta) }
            }

            AVSEvents.blocksWereMovedEvent.on {
                it, handler ->
                instance?.onBlocksMove(it.level, it.originalShip, it.newShip, it.oldCenter, it.newCenter, it.blocks)
            }
        }
    }
}