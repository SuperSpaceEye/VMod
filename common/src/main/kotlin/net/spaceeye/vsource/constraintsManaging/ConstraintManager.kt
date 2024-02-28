package net.spaceeye.vsource.constraintsManaging

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.constraintsManaging.types.MConstraint
import net.spaceeye.vsource.constraintsManaging.types.MConstraintTypes
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.events.AVSEvents
import net.spaceeye.vsource.utils.ServerClosable
import net.spaceeye.vsource.utils.ServerLevelHolder
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
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
    internal val constraintIdCounter = ConstraintIdCounter()

    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<MConstraint>>()
    private val groupedToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipIsStaticStatus = mutableMapOf<ShipId, Boolean>()

    private fun saveActiveConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = CompoundTag()
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values
        for ((shipId, mConstraints) in shipsConstraints) {
            if (!level.shipObjectWorld.allShips.contains(shipId)) {continue}

            val constraintsTag = ListTag()
            for (constraint in mConstraints) {
                if (!dimensionIds.contains(constraint.shipId0) &&
                    !level.shipObjectWorld.allShips.contains(constraint.shipId0)) {continue}

                val ctag = constraint.nbtSerialize() ?: continue
                ctag.putInt("MCONSTRAINT_TYPE", MConstraintTypes.typeToIdx(constraint.typeName) ?: continue)
                constraintsTag.add(ctag)
            }
            shipsTag.put(shipId.toString(), constraintsTag)
        }
        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        return tag
    }

    private fun saveNotLoadedConstraints(tag: CompoundTag): CompoundTag {
        val shipsTag = tag[SAVE_TAG_NAME_STRING]!! as CompoundTag
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        for ((shipId, groups) in groupedToLoadConstraints) {
            if (!level.shipObjectWorld.allShips.contains(shipId)) {continue}

            val constraintsTag = (shipsTag[shipId.toString()] ?: run {
                val tag = ListTag()
                shipsTag.put(shipId.toString(), tag)
                tag
            }) as ListTag
            for (group in groups) {
                if (group.wasSaved) {continue}

                for (constraint in group.constraintsToLoad) {
                    if (!dimensionIds.contains(constraint.shipId1) &&
                        !level.shipObjectWorld.allShips.contains(constraint.shipId1)) {continue}

                    val ctag = constraint.nbtSerialize() ?: continue
                    ctag.putInt("MCONSTRAINT_TYPE", MConstraintTypes.typeToIdx(constraint.typeName) ?: continue)
                    constraintsTag.add(ctag)
                }
                group.wasSaved = true
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

    private fun loadDataFromTag(shipsTag: CompoundTag) {
        val lastDimensionIds = loadDimensionIds(shipsTag)

        var maxId = -1
        for (shipId in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[shipId]!! as ListTag
            val constraints = mutableListOf<MConstraint>()
            for (ctag in shipConstraintsTag) {
                ctag as CompoundTag
                val mConstraint = MConstraintTypes
                    .idxToSupplier(ctag.getInt("MCONSTRAINT_TYPE"))
                    .get()
                    .nbtDeserialize(ctag, lastDimensionIds) ?: continue

                maxId = max(maxId, mConstraint.mID.id)

                constraints.add(mConstraint)
            }
            toLoadConstraints[shipId.toLong()] = constraints
        }
        constraintIdCounter.setCounter(maxId + 1)
    }

    private fun groupLoadedData() {
        val dimensionIds = level!!.shipObjectWorld.dimensionToGroundBodyIdImmutable.values
        for ((k, v) in toLoadConstraints) {
            val neededShipIds = mutableSetOf<ShipId>()
            for (constraint in v) {
                val secondIsDimension = dimensionIds.contains(constraint.shipId1)
                if (    !level.shipObjectWorld.allShips.contains(constraint.shipId0)
                    || (!level.shipObjectWorld.allShips.contains(constraint.shipId1)
                            && !secondIsDimension)) {continue}

                neededShipIds.add(constraint.shipId0)
                if (!secondIsDimension) {neededShipIds.add(constraint.shipId1)}
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
    fun makeConstraint(level: ServerLevel, constraint: MConstraint): ManagedConstraintId? {
        if (!constraint.onMakeMConstraint(level)) {return null}
        constraint.mID = constraintIdCounter.getID()

        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(constraint)
        idToConstraint[constraint.mID] = constraint

        setDirty()
        return constraint.mID
    }

    fun removeConstraint(level: ServerLevel, id: ManagedConstraintId): Boolean {
        val mCon = idToConstraint[id] ?: return false
        val shipConstraints = shipsConstraints[mCon.shipId0]!!
        mCon.onDeleteMConstraint(level)
        shipConstraints.remove(mCon)
        idToConstraint.remove(id)

        setDirty()
        return true
    }

    fun getAllConstraintsIdOfId(level: ServerLevel, shipId: ShipId): List<ManagedConstraintId> {
        val constraints = shipsConstraints[shipId] ?: return listOf()
        return constraints.map { it.mID }
    }

    @Internal
    fun makeConstraintWithId(level: ServerLevel, constraint: MConstraint, id: Int): ManagedConstraintId? {
        if (!constraint.onMakeMConstraint(level)) {return null}
        constraint.mID = ManagedConstraintId(id)

        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(constraint)
        idToConstraint[constraint.mID] = constraint

        setDirty()
        return constraint.mID
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
            level = ServerLevelHolder.overworldServerLevel!!
            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
            return instance!!
        }

        fun forceNewInstance(): ConstraintManager {
            level = ServerLevelHolder.overworldServerLevel!!
            instance = ServerLevelHolder.overworldServerLevel!!.dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
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