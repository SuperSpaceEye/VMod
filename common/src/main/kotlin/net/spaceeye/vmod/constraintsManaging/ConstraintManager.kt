package net.spaceeye.vmod.constraintsManaging

import dev.architectury.event.events.common.TickEvent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.constraintsManaging.types.MConstraint
import net.spaceeye.vmod.constraintsManaging.types.MConstraintTypes
import net.spaceeye.vmod.constraintsManaging.types.Tickable
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.utils.PosMap
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.max

//ShipId seem to be unique and are retained by ships after saving/loading

private const val SAVE_TAG_NAME_STRING = "vmod_ships_constraints"

@Internal
class ConstraintManager: SavedData() {
    // shipsConstraints and idToConstraint should share MConstraint
    private val shipsConstraints = mutableMapOf<ShipId, MutableList<MConstraint>>()
    private val idToConstraint = mutableMapOf<ManagedConstraintId, MConstraint>()
    internal val constraintIdCounter = ConstraintIdCounter()

    private val tickingConstraints = mutableListOf<Tickable>()

    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<MConstraint>>()
    private val groupedToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipIsStaticStatus = mutableMapOf<ShipId, Boolean>()

    private val posToMId = PosMap<ManagedConstraintId>()

    private var saveCounter = 0

    private fun saveActiveConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = CompoundTag()
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values
        for ((shipId, mConstraints) in shipsConstraints) {
            if (!allShips!!.contains(shipId)) {continue}

            val constraintsTag = ListTag()
            for (constraint in mConstraints) {
                if (constraint.saveCounter == saveCounter) { continue }
                if (!constraint.stillExists(allShips!!, dimensionIds)) { continue }

                val ctag = constraint.nbtSerialize() ?: run { WLOG("UNABLE TO SERIALIZE CONSTRAINT ${constraint.typeName} WITH ID ${constraint.mID}"); null } ?: continue
                ctag.putInt("MCONSTRAINT_TYPE", MConstraintTypes.typeToIdx(constraint.typeName) ?: run { WLOG("CONSTRAINT OF TYPE ${constraint.typeName} WAS NOT REGISTERED"); null } ?: continue)
                constraintsTag.add(ctag)
                constraint.saveCounter = saveCounter
            }
            shipsTag.put(shipId.toString(), constraintsTag)
        }
        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        return tag
    }

    private fun saveNotLoadedConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = tag[SAVE_TAG_NAME_STRING]!! as CompoundTag
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values

        for ((shipId, groups) in groupedToLoadConstraints) {
            if (!allShips!!.contains(shipId)) {continue}

            val constraintsTag = (shipsTag[shipId.toString()] ?:
                run {
                    val tag = ListTag()
                    shipsTag.put(shipId.toString(), tag)
                    tag
                }) as ListTag
            for (group in groups) {
                if (group.wasSaved) {continue}

                for (constraint in group.constraintsToLoad) {
                    if (!constraint.stillExists(allShips!!, dimensionIds)) { continue }

                    val ctag = constraint.nbtSerialize() ?: run { WLOG("UNABLE TO SERIALIZE CONSTRAINT ${constraint.typeName} WITH ID ${constraint.mID}"); null } ?: continue
                    ctag.putInt("MCONSTRAINT_TYPE", MConstraintTypes.typeToIdx(constraint.typeName) ?: run { WLOG("CONSTRAINT OF TYPE ${constraint.typeName} WAS NOT REGISTERED"); null } ?: continue)
                    constraintsTag.add(ctag)
                }
                group.wasSaved = true
            }
        }

        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        return tag
    }

    private fun saveDimensionIds(tag: CompoundTag): CompoundTag {
        val ids = dimensionToGroundBodyIdImmutable!!

        val idsTag = CompoundTag()
        for ((dimensionId, shipId) in ids) { idsTag.putLong(dimensionId, shipId) }

        (tag[SAVE_TAG_NAME_STRING] as CompoundTag).put("lastDimensionIds", idsTag)

        return tag
    }

    override fun save(tag: CompoundTag): CompoundTag {
        saveCounter++

        var tag = saveActiveConstraints(tag)
        tag = saveNotLoadedConstraints(tag)
        tag = saveDimensionIds(tag)
        tag = SynchronisedRenderingData.serverSynchronisedData.nbtSave(tag)

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

        tag.remove("lastDimensionIds")

        return ret
    }

    private fun loadDataFromTag(shipsTag: CompoundTag) {
        val lastDimensionIds = loadDimensionIds(shipsTag)

        var count = 0
        var maxId = -1
        for (shipId in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[shipId]!! as ListTag
            val constraints = mutableListOf<MConstraint>()
            for (ctag in shipConstraintsTag) {
                var type = -1
                var strType = "UNKNOWN"

                try {
                ctag as CompoundTag
                type = ctag.getInt("MCONSTRAINT_TYPE")
                strType = MConstraintTypes.idxToType(type)!!
                val mConstraint = MConstraintTypes
                    .idxToSupplier(ctag.getInt("MCONSTRAINT_TYPE"))
                    .get()
                    .nbtDeserialize(ctag, lastDimensionIds) ?: run { ELOG("FAILED TO DESEREALIZE CONSTRAINT OF TYPE ${MConstraintTypes.idxToSupplier(type).get().typeName}"); null } ?: continue

                maxId = max(maxId, mConstraint.mID.id)

                constraints.add(mConstraint)
                count++
                } catch (e: Exception) { ELOG("FAILED TO LOAD CONSTRAINT WITH IDX ${type} AND TYPE ${strType}") }
            }
            toLoadConstraints[shipId.toLong()] = constraints
        }
        constraintIdCounter.setCounter(maxId + 1)
        WLOG("LOADED $count CONSTRAINTS")
    }

    private fun groupLoadedData() {
        val dimensionIds = dimensionToGroundBodyIdImmutable!!.values
        for ((_, mConstraints) in toLoadConstraints) {
            val neededShipIds = mutableSetOf<ShipId>()
            for (constraint in mConstraints) {
                if (!constraint.stillExists(allShips!!, dimensionIds)) { continue }
                neededShipIds.addAll(constraint.attachedToShips(dimensionIds))
            }
            val group = LoadingGroup(level!!, mConstraints, neededShipIds, shipIsStaticStatus)
            for (id in neededShipIds) {
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

    // TODO redo this later
    private fun tryMakeConstraint(mCon: MConstraint, level: ServerLevel): Boolean {
        for (i in 0 until 1000) {
            if (mCon.onMakeMConstraint(level)) {return true}
        }
        ELOG("WAS NOT ABLE TO CREATE A CONSTRAINT OF TYPE ${mCon.typeName} UNDER ID ${mCon.mID}")
        return false
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    fun makeConstraint(level: ServerLevel, mCon: MConstraint): ManagedConstraintId? {
        mCon.mID = constraintIdCounter.getID()
        if (!tryMakeConstraint(mCon, level)) {constraintIdCounter.dec(); return null}

        mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipsConstraints.computeIfAbsent(it) { mutableListOf() }.add(mCon) }
        idToConstraint[mCon.mID] = mCon
        if (mCon is Tickable) { tickingConstraints.add(mCon) }
        mCon.getAttachmentPoints().forEach { posToMId.addItemTo(mCon.mID, it) }

        setDirty()
        return mCon.mID
    }

    fun getManagedConstraint(id: ManagedConstraintId): MConstraint? = idToConstraint[id]

    fun removeConstraint(level: ServerLevel, id: ManagedConstraintId): Boolean {
        val mCon = idToConstraint[id] ?: return false

        mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { (shipsConstraints[it] ?: return@forEach).remove(mCon) }
        mCon.onDeleteMConstraint(level)
        idToConstraint.remove(id)
        if (mCon is Tickable) { tickingConstraints.remove(mCon) }
        mCon.getAttachmentPoints().forEach { posToMId.removeItemFromPos(mCon.mID, it) }

        setDirty()
        return true
    }

    fun getAllConstraintsIdOfId(shipId: ShipId): List<ManagedConstraintId> {
        val constraints = shipsConstraints[shipId] ?: return listOf()
        return constraints.map { it.mID }
    }

    @Internal
    fun makeConstraintWithId(level: ServerLevel, mCon: MConstraint, id: Int): ManagedConstraintId? {
        if (id == -1) { ELOG("CREATING A CONSTRAINT WITH NO SPECIFIED ID WHEN A SPECIFIC ID IS EXPECTED AT ${constraintIdCounter.peekID()}"); return makeConstraint(level, mCon) }

        mCon.mID = ManagedConstraintId(id)
        if (!tryMakeConstraint(mCon, level)) {return null}

        mCon.attachedToShips(dimensionToGroundBodyIdImmutable!!.values).forEach { shipsConstraints.computeIfAbsent(it) { mutableListOf() }.add(mCon) }
        if (idToConstraint.contains(mCon.mID)) { ELOG("OVERWRITING AN ALREADY EXISTING CONSTRAINT IN makeConstraintWithId. SOMETHING PROBABLY WENT WRONG AS THIS SHOULDN'T HAPPEN.") }
        idToConstraint[mCon.mID] = mCon
        if (mCon is Tickable) { tickingConstraints.add(mCon) }
        mCon.getAttachmentPoints().forEach { posToMId.addItemTo(mCon.mID, it) }

        setDirty()
        return mCon.mID
    }

    fun tryGetIdOfPosition(pos: BlockPos): List<ManagedConstraintId>? {
        return posToMId.getItemsAt(pos)
    }

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

            SynchronisedRenderingData.serverSynchronisedData.nbtLoad(tag)
            if (tag.contains(SAVE_TAG_NAME_STRING) && tag[SAVE_TAG_NAME_STRING] is CompoundTag) {
                data.load(tag[SAVE_TAG_NAME_STRING]!! as CompoundTag)
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
        }
    }
}

object ConstraintManagerWatcher : ServerClosable() {
    override fun close() {
        ConstraintManager.close()
    }
}