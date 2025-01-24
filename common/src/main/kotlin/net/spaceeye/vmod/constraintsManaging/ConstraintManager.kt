package net.spaceeye.vmod.constraintsManaging

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.constraintsManaging.MConstraintTypes.getType
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.utils.PosMap
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.apache.commons.lang3.tuple.MutablePair
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import kotlin.math.max

//ShipId seem to be unique and are retained by ships after saving/loading

//TODO name
private const val SAVE_TAG_NAME_STRING = "vmod_VEntities"

@Internal
class ConstraintManager: SavedData() {
    // shipsConstraints and idToConstraint should share MConstraint
    private val shipsConstraints = mutableMapOf<ShipId, MutableList<MConstraint>>()
    private val idToConstraint = mutableMapOf<ManagedConstraintId, MConstraint>()
    internal val constraintIdCounter = ConstraintIdCounter()
    private val idToDisabledCollisions = mutableMapOf<ShipId, MutableMap<ShipId, MutablePair<Int, MutableList<(() -> Unit)?>>>>()

    private val tickingConstraints = Collections.synchronizedList(mutableListOf<Tickable>())

    private val toLoadConstraints = mutableListOf<MConstraint>()
    private val groupedToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipDataStatus = mutableMapOf<ShipId, ShipData>()

    private val posToMId = PosMap<ManagedConstraintId>()

    fun saveActiveConstraints(tag: CompoundTag): CompoundTag {
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        val constraintsTag = ListTag()
        idToConstraint.forEach { (_, it) -> saveConstraintToList(it, dimensionIds, constraintsTag) }
        tag.put(SAVE_TAG_NAME_STRING, constraintsTag)

        return tag
    }

    fun loadConstraintFromTag(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        var strType = "Unknown"

        try {
        strType = tag.getString("VEntityType")
        val data = tag.getCompound("data")
        return MConstraintTypes
            .strTypeToSupplier(strType)
            .get()
            .nbtDeserialize(data, lastDimensionIds) ?: run { ELOG("Failed to deserialize constraint of type $strType"); null }
        } catch (e: Exception) { ELOG("Failed to load constraint of type $strType\n${e.stackTraceToString()}")
        } catch (e: Error    ) { ELOG("Failed to load constraint of type $strType\n${e.stackTraceToString()}")}

        return null
    }

    private fun saveConstraintToList(
        constraint: MConstraint,
        dimensionIds: Collection<ShipId>,
        constraintsTag: ListTag
    ) {
        try {
            if (!constraint.stillExists(allShips!!, dimensionIds)) { return }

            val data = constraint.nbtSerialize() ?: run { WLOG("Unable to serialize constraint ${constraint.getType()} with ID ${constraint.mID}"); null } ?: return
            val ctag = CompoundTag()
            ctag.put("data", data)
            ctag.putString("VEntityType", constraint.getType())
            constraintsTag.add(ctag)
        } catch (e: Exception) { ELOG("Failed to save constraint of type ${constraint.getType()}\n${e.stackTraceToString()}")
        } catch (e: Error    ) { ELOG("Failed to save constraint of type ${constraint.getType()}\n${e.stackTraceToString()}")}
    }

    private fun saveNotLoadedConstraints(tag: CompoundTag): CompoundTag {
        val constraintsTag = tag[SAVE_TAG_NAME_STRING] as ListTag
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        val visited = mutableSetOf<LoadingGroup>()
        val visitedConstraints = mutableSetOf<Int>()
        for ((_, groups) in groupedToLoadConstraints) {
            for (group in groups) {
                if (group.wasSaved) {continue}
                if (visited.contains(group)) {continue}
                visited.add(group)
                for (constraint in group.constraintsToLoad) {
                    if (visitedConstraints.contains(constraint.mID)) {continue}
                    visitedConstraints.add(constraint.mID)
                    saveConstraintToList(constraint, dimensionIds, constraintsTag)

                    if (!constraint.stillExists(allShips!!, dimensionIds)) {continue}
                }
            }
        }

        return tag
    }

    private fun saveDimensionIds(tag: CompoundTag): CompoundTag {
        val ids = dimensionToGroundBodyIdImmutable!!

        val idsTag = CompoundTag()
        for ((dimensionId, shipId) in ids) { idsTag.putLong(dimensionId, shipId) }

        tag.put("lastDimensionIds", idsTag)

        return tag
    }

    override fun save(tag: CompoundTag): CompoundTag {
        var tag = saveDimensionIds(tag)
        tag = saveActiveConstraints(tag)
        tag = saveNotLoadedConstraints(tag)

        instance = null
        return tag
    }

    //It's loading them the other way around because it needs to get dimensionId from saved shipId
    private fun loadDimensionIds(tag: CompoundTag): Map<Long, String> {
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
        val constraintsTag = tag[SAVE_TAG_NAME_STRING] as ListTag

        var count = 0
        var maxId = -1
        for (ctag in constraintsTag) {
            toLoadConstraints.add(loadConstraintFromTag(ctag as CompoundTag, lastDimensionIds) ?: continue)
            count++
            maxId = max(maxId, toLoadConstraints.last().mID)
        }
        constraintIdCounter.setCounter(maxId + 1)
        WLOG("Deserialized $count constraints")
    }

    private fun groupLoadedData() {
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        val groups = mutableMapOf<MutableSet<Long>, MutableList<MConstraint>>()

        for (constraint in toLoadConstraints) {
            if (!constraint.stillExists(allShips!!, dimensionIds)) { continue }
            val neededIds = constraint.attachedToShips(dimensionIds).toMutableSet()
            groups.getOrPut(neededIds) { mutableListOf() }.add(constraint)
        }

        for ((neededIds, toLoad) in groups) {
            val group = LoadingGroup(level!!, toLoad, neededIds, shipDataStatus)
            for (id in neededIds) {
                groupedToLoadConstraints.computeIfAbsent(id) { mutableListOf() }.add(group)
            }
        }
        toLoadConstraints.clear()
    }

    private fun setLoadedId(ship: ServerShip) {
        if (!groupedToLoadConstraints.containsKey(ship.id)) {return}

        //groups are shared between groupedToLoadConstraints, so just notify all groups belonging to this id and just delete all of them
        for (group in groupedToLoadConstraints[ship.id]!!) {
            group.setLoadedId(ship)
        }

        groupedToLoadConstraints.remove(ship.id)
    }

    private fun createConstraints() {
        VSEvents.shipLoadEvent.on { (ship), handler ->
            setLoadedId(ship)
            if (groupedToLoadConstraints.isEmpty()) {
                handler.unregister()
            }
        }
    }

    fun load(tag: CompoundTag) {
        loadDataFromTag(tag)
        groupLoadedData()
        createConstraints()
    }

    private fun tryMakeConstraint(mCon: MConstraint, level: ServerLevel, onFailure: () -> Unit, rest: () -> Unit) {
        for (i in 0 until 10) {
            if (mCon.onMakeMConstraint(level)) {return rest()}
        }

        var attempts = 0
        val maxAttempts = VMConfig.SERVER.CONSTRAINT_CREATION_ATTEMPTS
        RandomEvents.serverOnTick.on { _, unsubscribe ->
            if (attempts > maxAttempts) {
                onFailure()
                return@on unsubscribe()
            }

            if (mCon.onMakeMConstraint(level)) {
                rest()
                return@on unsubscribe()
            }

            attempts++
        }
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    fun makeConstraint(level: ServerLevel, mCon: MConstraint, callback: ((ManagedConstraintId?) -> Unit)) {
        mCon.mID = constraintIdCounter.getID()

        tryMakeConstraint(mCon, level, {
            ELOG("Was not able to create constraint of type ${mCon.getType()} under ID ${mCon.mID}")
            callback(null)
        }) {
            mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipsConstraints.computeIfAbsent(it) { mutableListOf() }.add(mCon) }
            idToConstraint[mCon.mID] = mCon
            if (mCon is Tickable) { tickingConstraints.add(mCon) }
            mCon.getAttachmentPositions().forEach { posToMId.addItemTo(mCon.mID, it) }

            setDirty()

            callback(mCon.mID)
        }
    }

    fun getManagedConstraint(id: ManagedConstraintId): MConstraint? = idToConstraint[id]

    fun removeConstraint(level: ServerLevel, id: ManagedConstraintId): Boolean {
        val mCon = idToConstraint[id] ?: return false

        mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { (shipsConstraints[it] ?: return@forEach).remove(mCon) }
        mCon.onDeleteMConstraint(level)
        idToConstraint.remove(id)
        if (mCon is Tickable) { tickingConstraints.remove(mCon) }
        mCon.getAttachmentPositions().forEach { posToMId.removeItemFromPos(mCon.mID, it) }

        setDirty()
        return true
    }

    fun getAllConstraintsIdOfId(shipId: ShipId): List<ManagedConstraintId> {
        val constraints = shipsConstraints[shipId] ?: return listOf()
        return constraints.map { it.mID }
    }

    @Internal
    fun makeConstraintWithId(level: ServerLevel, mCon: MConstraint, id: Int, callback: ((ManagedConstraintId?) -> Unit)) {
        if (id == -1) { throw AssertionError("makeConstraintWithId was called without a specific id") }

        mCon.mID = id
        tryMakeConstraint(mCon, level, {
            ELOG("Was not able to create constraint of type ${mCon.getType()} under ID ${mCon.mID}")
            callback(null)
        }) {
            mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipsConstraints.computeIfAbsent(it) { mutableListOf() }.add(mCon) }
            if (idToConstraint.contains(mCon.mID)) { ELOG("OVERWRITING AN ALREADY EXISTING CONSTRAINT IN makeConstraintWithId. SOMETHING PROBABLY WENT WRONG AS THIS SHOULDN'T HAPPEN.") }
            idToConstraint[mCon.mID] = mCon
            if (mCon is Tickable) { tickingConstraints.add(mCon) }
            mCon.getAttachmentPositions().forEach { posToMId.addItemTo(mCon.mID, it) }

            setDirty()
            callback(mCon.mID)
        }
    }

    fun tryGetIdsOfPosition(pos: BlockPos): List<ManagedConstraintId>? {
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
        val constraints = getAllConstraintsIdOfId(originalShip.id)
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
                    val constraint = getManagedConstraint(id) ?: continue

                    constraint.attachedToShips(dimensionIds).forEach { shipsConstraints[it]?.remove(constraint) }
                    constraint.moveShipyardPosition(level!!, fromPos, toPos, newShip.id)
                    constraint.attachedToShips(dimensionIds).forEach { shipsConstraints.getOrPut(it) { mutableListOf() }.add(constraint) }

                    setDirty()
                }
            }
        }
    }

    companion object {
        private var instance: ConstraintManager? = null
        private var level: ServerLevel? = null

        // SavedData is saved after VSPhysicsPipelineStage is deleted, so getting allShips and
        // dimensionToGroundBodyIdImmutable from level.shipObjectWorld is impossible, unless you get it's reference
        // before it got deleted
        var dimensionToGroundBodyIdImmutable: Map<DimensionId, ShipId>? = null
        var allShips: QueryableShipData<Ship>? = null

        init {
            makeServerEvents()
            ConstraintManagerWatcher // to initialize watcher
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

        fun getInstance(): ConstraintManager {
            if (instance != null) {return instance!!}
            level = ServerLevelHolder.overworldServerLevel!!

            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun initNewInstance(): ConstraintManager {
            level = ServerLevelHolder.overworldServerLevel!!

            dimensionToGroundBodyIdImmutable = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable
            allShips = level!!.shipObjectWorld.allShips

            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VM.MOD_ID)
            return instance!!
        }

        fun create(): ConstraintManager {
            return ConstraintManager()
        }

        fun load(tag: CompoundTag): ConstraintManager {
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
                instance.getAllConstraintsIdOfId(ship.id).forEach {
                    instance.removeConstraint(level!!, it)
                }
                instance.setDirty()
            }

            TickEvent.SERVER_PRE.register {
                server ->
                getInstance()
                if (instance!!.tickingConstraints.isEmpty()) {return@register}

                val toRemove = mutableListOf<Tickable>()
                instance!!.tickingConstraints.forEach {
                    it.tick(server) {toRemove.add(it)}
                }
                instance!!.tickingConstraints.removeAll(toRemove)
                setDirty()
            }

            AVSEvents.splitShip.on {
                it, handler ->
                instance?.shipWasSplitEvent(it.originalShip, it.newShip, it.centerBlock, it.blocks)
            }

            ShipSchematic.registerCopyPasteEvents(
                    "VMod Constraint Manager",
            {level: ServerLevel, shipsToBeSaved: List<ServerShip>, globalMap: MutableMap<String, Any>, unregister: () -> Unit ->
                val instance = getInstance()

                val toSave = shipsToBeSaved.filter { instance.shipsConstraints.containsKey(it.id) }
                if (toSave.isEmpty()) {return@registerCopyPasteEvents null}

                val constraintsToSave = toSave.mapNotNull { instance.shipsConstraints[it.id] }.flatten().toSet()
                val tag = CompoundTag()
                tag.put(SAVE_TAG_NAME_STRING, ListTag().also { tag ->
                    constraintsToSave.forEach { instance.saveConstraintToList(it, dimensionToGroundBodyIdImmutable!!.values, tag) } })
                instance.saveDimensionIds(tag)

                CompoundTagSerializable(tag)
            }, { level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, loadFile: ISerializable?, globalMap: MutableMap<String, Any>, unregister: () -> Unit ->

                if (loadFile == null) {return@registerCopyPasteEvents}
                val instance = getInstance()

                val tag = CompoundTagSerializable(CompoundTag()).also { it.deserialize(loadFile.serialize()) }.tag!!
                val lastDimensionIds = instance.loadDimensionIds(tag)
                val toInitConstraints = (tag[SAVE_TAG_NAME_STRING] as ListTag).mapNotNull { instance.loadConstraintFromTag(it as CompoundTag, lastDimensionIds) }

                val mapped = loadedShips.associate {
                    if (lastDimensionIds.containsKey(it.second)) {
                        Pair(level.shipObjectWorld.dimensionToGroundBodyIdImmutable[lastDimensionIds[it.second]]!!, it.first.id)
                    } else {
                        Pair(it.second, it.first.id)
                    }
                }

                val changedIds = mutableMapOf<Int, Int>()
                for (it in toInitConstraints) {
                    level.makeManagedConstraint(it.copyMConstraint(level, mapped) ?: continue) {newId ->
                        changedIds[it.mID] = newId ?: return@makeManagedConstraint
                    }
                }

                globalMap["changedIDs"] = changedIds
            }
            )
        }
    }
}

object ConstraintManagerWatcher : ServerClosable() {
    override fun close() {
        ConstraintManager.close()
    }
}