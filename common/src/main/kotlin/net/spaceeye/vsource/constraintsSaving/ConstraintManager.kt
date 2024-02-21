package net.spaceeye.vsource.constraintsSaving

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.events.AVSEvents
import net.spaceeye.vsource.utils.MPair
import net.spaceeye.vsource.utils.ServerClosable
import net.spaceeye.vsource.utils.ServerLevelHolder
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.max

//ShipId seem to be unique and are retained by ships after saving/loading

private const val SAVE_TAG_NAME_STRING = "vsource_ships_constraints"

@Internal
class ConstraintManager: SavedData() {
    // shipsConstraints and idToConstraint should share MConstraint
    private val shipsConstraints = mutableMapOf<ShipId, MutableList<MConstraint>>()
    private val idToConstraint = mutableMapOf<ManagedConstraintId, MConstraint>()
    private val constraintIdManager = ConstraintIdManager()

    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<MPair<VSConstraint, Int>>>()
    private val groupedToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipIsStaticStatus = mutableMapOf<ShipId, Boolean>()

    class MConstraint(
        var it: VSConstraint,
        var mID: ManagedConstraintId
    ) {
        val vsID: Int
            get() {
                val idManager = getInstance().constraintIdManager
                return idManager.getVSid(mID) ?: -1
            }
    }

    private fun saveActiveConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = CompoundTag()
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values
        for ((shipId, mConstraints) in shipsConstraints) {
            if (!level.shipObjectWorld.allShips.contains(shipId)) {continue}

            val constraintsTag = ListTag()
            for (constraint in mConstraints) {
                if (!dimensionIds.contains(constraint.it.shipId1) &&
                    !level.shipObjectWorld.allShips.contains(constraint.it.shipId1)) {continue}

                val ctag = VSConstraintSerializationUtil.serializeConstraint(constraint.it) ?: continue
                ctag.putInt("managedID", constraint.mID.id)
                constraintsTag.add(ctag)
            }
            shipsTag.put(shipId.toString(), constraintsTag)
        }
        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        return tag
    }

    //TODO i don't like how it works but i don't care atm
    private fun saveNotLoadedConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = tag[SAVE_TAG_NAME_STRING]!!
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        val savedGroups = mutableSetOf<LoadingGroup>()
        for ((shipId, groups) in groupedToLoadConstraints) {
            if (!level.shipObjectWorld.allShips.contains(shipId)) {continue}

            val constraintsTag = ListTag()
            for (group in groups) {
                if (savedGroups.contains(group)) {continue}

                for (constraint in group.constraintsToLoad) {
                    if (!dimensionIds.contains(constraint.first.shipId1) &&
                        !level.shipObjectWorld.allShips.contains(constraint.first.shipId1)) {continue}

                    val ctag = VSConstraintSerializationUtil.serializeConstraint(constraint.first) ?: continue
                    ctag.putInt("managedID", constraint.second)
                    constraintsTag.add(ctag)
                }
                savedGroups.add(group)
            }
        }

        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        return tag
    }

    private fun saveDimensionIds(tag: CompoundTag): CompoundTag {
        val ids = level.shipObjectWorld.dimensionToGroundBodyIdImmutable

        val idsTag = CompoundTag()
        for ((dimensionId, shipId) in ids) { idsTag.putLong(dimensionId, shipId) }

        (tag[SAVE_TAG_NAME_STRING] as CompoundTag).put("lastDimensionIds", idsTag)

        return tag
    }

    override fun save(tag: CompoundTag): CompoundTag {
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
            return level.shipObjectWorld.dimensionToGroundBodyIdImmutable.map { (k, v) -> Pair(v, k) }.toMap()
        }

        val dtag = tag["lastDimensionIds"] as CompoundTag
        for (dimensionId in dtag.allKeys) { ret[dtag.getLong(dimensionId)] = dimensionId }

        tag.remove("lastDimensionIds")

        return ret
    }

    //if constraint is between world and ship, then world's id should be in the second shipId of the constraint
    private fun tryConvertDimensionId(ctag: CompoundTag, lastDimensionIds: Map<Long, String>) {
        if (!ctag.contains("shipId1")) {return}

        val id = ctag.getLong("shipId1")
        val dimensionIdStr = lastDimensionIds[id] ?: return
        ctag.putLong("shipId1", level.shipObjectWorld.dimensionToGroundBodyIdImmutable[dimensionIdStr]!!)
    }

    private fun loadDataFromTag(shipsTag: CompoundTag) {
        val lastDimensionIds = loadDimensionIds(shipsTag)

        var maxId = -1
        for (shipId in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[shipId]!! as ListTag
            val constraints = mutableListOf<MPair<VSConstraint, Int>>()
            for (ctag in shipConstraintsTag) {
                ctag as CompoundTag
                tryConvertDimensionId(ctag, lastDimensionIds)
                val constraint = VSConstraintDeserializationUtil.deserializeConstraint(ctag) ?: continue
                val id = if (ctag.contains("managedID")) ctag.getInt("managedID") else -1

                maxId = max(maxId, id)

                constraints.add(MPair(constraint, id))
            }
            toLoadConstraints[shipId.toLong()] = constraints
        }
        constraintIdManager.setCounter(maxId + 1)
    }

    private fun groupLoadedData() {
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values
        for ((k, v) in toLoadConstraints) {
            val neededShipIds = mutableSetOf<ShipId>()
            for (constraint in v) {
                val secondIsDimension = dimensionIds.contains(constraint.first.shipId1)
                if (    !level.shipObjectWorld.allShips.contains(constraint.first.shipId0)
                    || (!level.shipObjectWorld.allShips.contains(constraint.first.shipId1)
                            && !secondIsDimension)) {continue}

                neededShipIds.add(constraint.first.shipId0)
                if (!secondIsDimension) {neededShipIds.add(constraint.first.shipId1)}
            }
            val group = LoadingGroup(level!!, v, neededShipIds, shipIsStaticStatus)
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

    //TODO REMEMBER TO FUCKING CALL setDirty()
    //VERY IMPORTANT!!!! IF CONSTRAINT IS BETWEEN SHIP AND WORLD, WORLD ID SHOULD BE THE shipId1 !!!!!
    fun makeConstraint(level: ServerLevel, constraint: VSConstraint): ManagedConstraintId? {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint) ?: return null
        val managedId = constraintIdManager.addVSid(constraintId)

        val newMCon = MConstraint(constraint, managedId)
        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(newMCon)
        idToConstraint[managedId] = newMCon

        setDirty()
        return managedId
    }

    fun removeConstraint(level: ServerLevel, id: ManagedConstraintId): Boolean {
        val vsId = constraintIdManager.getVSid(id) ?: return false
        if (!level.shipObjectWorld.removeConstraint(vsId)) {return false}

        val mCon = idToConstraint[id] ?: return false
        val shipConstraints = shipsConstraints[mCon.it.shipId0] ?: return false
        shipConstraints.remove(mCon)
        idToConstraint.remove(id)

        setDirty()
        return true
    }

    fun updateConstraint(level: ServerLevel, id: ManagedConstraintId, constraint: VSConstraint): Boolean {
        val vsId = constraintIdManager.getVSid(id) ?: return false
        if (!level.shipObjectWorld.updateConstraint(vsId, constraint)) {return false}

        val mCon = idToConstraint[id] ?: return false
        mCon.it = constraint

        setDirty()
        return true
    }

    fun getAllConstraintsIdOfId(level: ServerLevel, shipId: ShipId): List<ManagedConstraintId> {
        val constraints = shipsConstraints[shipId] ?: return listOf()
        return constraints.map { it.mID }
    }

    @Internal
    fun makeConstraintWithId(level: ServerLevel, constraint: VSConstraint, id: Int): ManagedConstraintId? {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint) ?: return null

        val managedId = constraintIdManager.setVSid(constraintId, id)

        val newMCon = MConstraint(constraint, managedId)
        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(newMCon)
        idToConstraint[managedId] = newMCon

        setDirty()
        return managedId
    }

    companion object {
        private var instance: ConstraintManager? = null
        private var level: ServerLevel? = null

        init {
            makeServerEvents()
            ConstraintManagerWatcher // to initialize watcher
        }

        fun close() {
            instance = null
            level = null
        }

        fun getInstance(): ConstraintManager {
            if (instance != null) {return instance!!}
            level = ServerLevelHolder.serverLevel!!
            instance = ServerLevelHolder.serverLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
            return instance!!
        }

        fun forceNewInstance(): ConstraintManager {
            level = ServerLevelHolder.serverLevel!!
            instance = ServerLevelHolder.serverLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
            return instance!!
        }

        fun create(): ConstraintManager {
            return ConstraintManager()
        }

        fun load(tag: CompoundTag): ConstraintManager {
            val data = create()

            if (tag.contains(SAVE_TAG_NAME_STRING) && tag[SAVE_TAG_NAME_STRING] is CompoundTag) {
                data.load(tag[SAVE_TAG_NAME_STRING]!! as CompoundTag)
            }
            SynchronisedRenderingData.serverSynchronisedData.nbtLoad(tag)

            return data
        }

        private fun makeServerEvents() {
            AVSEvents.serverShipRemoveEvent.on {
                (ship), handler ->
                if (level == null) { return@on }
                getInstance().setDirty()
            }
        }
    }
}

object ConstraintManagerWatcher : ServerClosable() {
    override fun close() {
        ConstraintManager.close()
    }
}