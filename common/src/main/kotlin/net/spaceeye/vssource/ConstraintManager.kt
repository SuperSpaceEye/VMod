package net.spaceeye.vssource

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vssource.utils.VSConstraintDeserializationUtil
import net.spaceeye.vssource.utils.VSConstraintSerializationUtil
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
) {
    //boolean is for isStatic status before loading
    private val shipRefs: MutableList<Pair<ServerShip, Boolean>> = mutableListOf()

    fun setLoadedId(ship: ServerShip) {
        if (neededShipIds.isEmpty()) {return}
        if (!neededShipIds.remove(ship.id)) { return }

        shipRefs.add(Pair(ship, ship.isStatic))
        ship.isStatic = true // so that ships don't drift while shit is being loaded

        if (neededShipIds.isEmpty()) {
            applyConstraints()

            constraintsToLoad.clear()
            shipRefs.clear()
        }
    }

    private fun applyConstraints() {
        for (constraint in constraintsToLoad) {
            level.shipObjectWorld.createNewConstraint(constraint)
        }
        for (ship in shipRefs) {
            ship.first.isStatic = ship.second
        }
    }
}

//ShipId seem to be unique and are retained by ships after saving/loading

//TODO put constants into constants class
class ConstraintManager: SavedData() {
    private val shipConstraints = mutableMapOf<ShipId, MutableList<Pair<VSConstraint, VSConstraintId>>>()
    private val toLoadConstraints = mutableMapOf<ShipId, MutableList<VSConstraint>>()
    private val clusteredToLoadConstraints = mutableMapOf<ShipId, MutableList<LoadingCluster>>()

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
        tag.put("vs_source_ships_constraints", shipsTag)
        return tag
    }

    private fun loadDataFromTag(tag: CompoundTag) {
        val shipsTag = tag["vs_source_ships_constraints"]!! as CompoundTag
        for (k in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[k]!! as CompoundTag
            val constraints = mutableListOf<VSConstraint>()
            for (kk in shipConstraintsTag.allKeys) {
                val ctag = shipConstraintsTag[k]!! as CompoundTag
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
            val cluster = LoadingCluster(level!!, v, neededShipIds)
            for (id in neededShipIds) {
                clusteredToLoadConstraints.computeIfAbsent(id) { mutableListOf() }.add(cluster)
            }
        }
    }

    private fun createConstraints() {
        VSEvents.shipLoadEvent.on { (ship), handler ->
            handler.unregister()
            val id = ship.id
            if (!clusteredToLoadConstraints.containsKey(id)) {return@on}
            for (cluster in clusteredToLoadConstraints[id]!!) {
                cluster.setLoadedId(ship)
            }
        }
    }

    fun load(tag: CompoundTag) {
        loadDataFromTag(tag)
        clusterLoadedData()
        createConstraints()
    }

    fun makeConstraint(level: ServerLevel, constraint: VSConstraint): VSConstraintId {
        val constraintId = level.shipObjectWorld.createNewConstraint(constraint)!!
        shipConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(Pair(constraint, constraintId))
        shipConstraints.computeIfAbsent(constraint.shipId1) { mutableListOf() }.add(Pair(constraint, constraintId))
        return constraintId
    }

    companion object {
        private var instance: ConstraintManager? = null
        private var level: ServerLevel? = null

        fun getInstance(level: Level): ConstraintManager {
            if (instance != null) {return instance!!}
            level as ServerLevel
            this.level = level
            instance = level.server.overworld().dataStorage.computeIfAbsent(::load, ::create, VSS.MOD_ID)
            return instance!!
        }

        fun create(): ConstraintManager {return ConstraintManager() }
        fun load(tag: CompoundTag): ConstraintManager {
            val data = create()

            if (tag.contains("vs_source_ships_constraints") && tag["vs_source_ships_constraints"] is CompoundTag) {
                data.load(tag["vs_source_ships_constraints"]!! as CompoundTag)
            }

            return data
        }
    }
}