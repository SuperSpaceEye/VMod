package net.spaceeye.vsource.utils.constraintsSaving

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.utils.MPair
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld

//ShipId seem to be unique and are retained by ships after saving/loading

private const val SAVE_TAG_NAME_STRING = "vsource_ships_constraints"

@Internal
class ConstraintManager: SavedData() {
    private val shipsConstraints = mutableMapOf<ShipId, MutableList<MPair<VSConstraint, ManagedConstraintId>>>()
    private val idToConstraint = mutableMapOf<ManagedConstraintId, MPair<VSConstraint, ManagedConstraintId>>()
    private val constraintIdManager = ConstraintIdManager()

    //TODO think of a way to clear these things
    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<MPair<VSConstraint, Int>>>()
    private val groupedToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingGroup>>()
    private val shipIsStaticStatus = mutableMapOf<ShipId, Boolean>()

    //TODO check if ship id exists before saving constraint
    override fun save(tag: CompoundTag): CompoundTag {
        val shipsTag = CompoundTag()
        for ((k, v) in shipsConstraints) {
            val constraintsTag = CompoundTag()
            for ((i, constraint) in v.withIndex()) {
                val ctag = VSConstraintSerializationUtil.serializeConstraint(constraint.first) ?: continue
                ctag.putInt("managedID", constraint.second.id)
                constraintsTag.put(i.toString(), ctag)
            }
            shipsTag.put(k.toString(), constraintsTag)
        }
        tag.put(SAVE_TAG_NAME_STRING, shipsTag)

        instance = null

        return tag
    }

    private fun loadDataFromTag(shipsTag: CompoundTag) {
        for (k in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[k]!! as CompoundTag
            val constraints = mutableListOf<MPair<VSConstraint, Int>>()
            for (kk in shipConstraintsTag.allKeys) {
                val ctag = shipConstraintsTag[kk]!! as CompoundTag
                val constraint = VSConstraintDeserializationUtil.deserializeConstraint(ctag) ?: continue
                val id = if (ctag.contains("managedID")) ctag.getInt("managedID") else -1

                constraints.add(MPair(constraint, id))
            }
            toLoadConstraints[k.toLong()] = constraints
        }
    }

    private fun groupLoadedData() {
        for ((k, v) in toLoadConstraints) {
            val neededShipIds = mutableSetOf<ShipId>()
            for (constraint in v) {
                neededShipIds.add(constraint.first.shipId0)
                neededShipIds.add(constraint.first.shipId1)
            }
            val cluster = LoadingGroup(level!!, v, neededShipIds, shipIsStaticStatus)
            for (id in neededShipIds) {
                groupedToLoadConstraints.computeIfAbsent(id) { mutableListOf() }.add(cluster)
            }
        }
        toLoadConstraints.clear()
    }

    private fun setLoadedId(ship: ServerShip) {
        if (!groupedToLoadConstraints.containsKey(ship.id)) {return}
        for (cluster in groupedToLoadConstraints[ship.id]!!) {
            cluster.setLoadedId(ship)
        }
    }

    private fun createConstraints() {
        VSEvents.shipLoadEvent.on { (ship), handler ->
            setLoadedId(ship)
        }
    }

    fun load(tag: CompoundTag) {
        loadDataFromTag(tag)
        groupLoadedData()
        createConstraints()
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    fun makeConstraint(level: ServerLevel, constraint: VSConstraint): ManagedConstraintId? {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint) ?: return null
        val managedId = constraintIdManager.addVSid(constraintId)

        val newPair = MPair(constraint, managedId)
        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(newPair)
        idToConstraint[managedId] = newPair

        setDirty()
        return managedId
    }

    fun removeConstraint(level: ServerLevel, id: ManagedConstraintId): Boolean {
        val vsId = constraintIdManager.getVSid(id) ?: return false
        if (!level.shipObjectWorld.removeConstraint(vsId)) {return false}

        val pair = idToConstraint[id] ?: return false
        val shipConstraints = shipsConstraints[pair.first.shipId0] ?: return false
        shipConstraints.remove(pair)
        idToConstraint.remove(id)

        setDirty()
        return true
    }

    fun updateConstraint(level: ServerLevel, id: ManagedConstraintId, constraint: VSConstraint): Boolean {
        val vsId = constraintIdManager.getVSid(id) ?: return false
        if (!level.shipObjectWorld.updateConstraint(vsId, constraint)) {return false}

        val pair = idToConstraint[id] ?: return false
        pair.first = constraint

        setDirty()
        return true
    }

    fun getAllConstraintsIdOfId(level: ServerLevel, shipId: ShipId): List<ManagedConstraintId> {
        val constraints = shipsConstraints[shipId] ?: return listOf()
        return constraints.map { it.second }
    }

    @Internal
    fun makeConstraintWithId(level: ServerLevel, constraint: VSConstraint, id: Int): ManagedConstraintId? {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint) ?: return null

        val managedId = constraintIdManager.setVSid(constraintId, id)

        val newPair = MPair(constraint, managedId)
        shipsConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(newPair)
        idToConstraint[managedId] = newPair

        setDirty()
        return managedId
    }

    companion object {
        private var instance: ConstraintManager? = null
        private var level: ServerLevel? = null

        //TODO look closer into this
        fun getInstance(level: Level): ConstraintManager {
            if (instance != null) {return instance!!}
            level as ServerLevel
            Companion.level = level
            instance = level.server.overworld().dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
            return instance!!
        }

        fun forceNewInstance(level: ServerLevel): ConstraintManager {
            Companion.level = level
            instance = level.server.overworld().dataStorage.computeIfAbsent(Companion::load, Companion::create, VS.MOD_ID)
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

            return data
        }
    }
}