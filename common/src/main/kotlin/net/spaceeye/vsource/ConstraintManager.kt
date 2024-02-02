package net.spaceeye.vsource

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vsource.utils.VSConstraintDeserializationUtil
import net.spaceeye.vsource.utils.VSConstraintSerializationUtil
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld

inline fun mMakeConstraint(level: ServerLevel, constraint: VSConstraint) {
    ConstraintManager.getInstance(level).makeConstraint(level, constraint)
}

private class LoadingCluster(
    val level: ServerLevel,
    val constraintsToLoad: MutableList<VSConstraint>,
    val neededShipIds: MutableSet<ShipId>,
    val shipIsStaticStatus: MutableMap<ShipId, Boolean>
) {
    //boolean is for isStatic status before loading
    private val shipRefs: MutableMap<ShipId, ServerShip> = mutableMapOf()

    fun setLoadedId(ship: ServerShip) {
        if (neededShipIds.isEmpty()) {return}
        if (!neededShipIds.remove(ship.id)) { return }

        shipRefs.computeIfAbsent(ship.id) {ship}
        shipIsStaticStatus.computeIfAbsent(ship.id) {ship.isStatic}
        ship.isStatic = true // so that ships don't drift while ships are being loaded

        if (neededShipIds.isEmpty()) {
            applyConstraints()

            constraintsToLoad.clear()
            shipRefs.clear()
        }
    }

    private fun applyConstraints() {
        for (constraint in constraintsToLoad) {
            mMakeConstraint(level, constraint)
        }
        for ((k, ship) in shipRefs) {
            ship.isStatic = shipIsStaticStatus[ship.id] ?: continue
            shipIsStaticStatus.remove(ship.id)
        }
    }
}

//ShipId seem to be unique and are retained by ships after saving/loading

//TODO put constants into constants class
class ConstraintManager: SavedData() {
    private val shipConstraints = mutableMapOf<ShipId, MutableList<Pair<VSConstraint, VSConstraintId>>>()
    //TODO think of a way to clear these things
    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<VSConstraint>>()
    private val clusteredToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingCluster>>()
    private val shipIsStaticStatus = mutableMapOf<ShipId, Boolean>()

    //TODO check if ship id exists before saving constraint
    override fun save(tag: CompoundTag): CompoundTag {
        val shipsTag = CompoundTag()
        for ((k, v) in shipConstraints) {
            val constraintsTag = CompoundTag()
            for ((i, constraint) in v.withIndex()) {
                val ctag = VSConstraintSerializationUtil.serializeConstraint(constraint.first) ?: continue
                constraintsTag.put(i.toString(), ctag)
            }
            shipsTag.put(k.toString(), constraintsTag)
        }
        tag.put("vsource_ships_constraints", shipsTag)

        instance = null

        return tag
    }

    private fun loadDataFromTag(shipsTag: CompoundTag) {
        for (k in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[k]!! as CompoundTag
            val constraints = mutableListOf<VSConstraint>()
            for (kk in shipConstraintsTag.allKeys) {
                val ctag = shipConstraintsTag[kk]!! as CompoundTag
                val constraint = VSConstraintDeserializationUtil.deserializeConstraint(ctag) ?: continue
                constraints.add(constraint)
            }
            toLoadConstraints[k.toLong()] = constraints
        }
    }

    private fun clusterLoadedData() {
        for ((k, v) in toLoadConstraints) {
            val neededShipIds = mutableSetOf<ShipId>()
            for (constraint in v) {
                neededShipIds.add(constraint.shipId0)
                neededShipIds.add(constraint.shipId1)
            }
            val cluster = LoadingCluster(level!!, v, neededShipIds, shipIsStaticStatus)
            for (id in neededShipIds) {
                clusteredToLoadConstraints.computeIfAbsent(id) { mutableListOf() }.add(cluster)
            }
        }
    }

    fun setLoadedId(ship: ServerShip) {
        if (!clusteredToLoadConstraints.containsKey(ship.id)) {return}
        for (cluster in clusteredToLoadConstraints[ship.id]!!) {
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
        clusterLoadedData()
        createConstraints()
    }

    //TODO REMEMBER TO FUCKING CALL setDirty()
    //TODO figure out how to maintain VSConstraintId after save/load
    fun makeConstraint(level: ServerLevel, constraint: VSConstraint): VSConstraintId {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint)!!
        shipConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(Pair(constraint, constraintId))
        setDirty()
        return constraintId
    }

    companion object {
        private var instance: ConstraintManager? = null
        private var level: ServerLevel? = null

        //TODO look closer into this
        fun getInstance(level: Level): ConstraintManager {
            if (instance != null) {return instance!!}
            level as ServerLevel
            this.level = level
            instance = level.server.overworld().dataStorage.computeIfAbsent(::load, ::create, VS.MOD_ID)
            return instance!!
        }

        fun create(): ConstraintManager {
            return ConstraintManager()
        }
        fun load(tag: CompoundTag): ConstraintManager {
            val data = create()

            if (tag.contains("vsource_ships_constraints") && tag["vsource_ships_constraints"] is CompoundTag) {
                data.load(tag["vsource_ships_constraints"]!! as CompoundTag)
            }

            return data
        }
    }
}