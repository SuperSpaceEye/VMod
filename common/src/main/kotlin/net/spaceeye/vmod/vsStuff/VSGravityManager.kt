package net.spaceeye.vmod.vsStuff

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.shipForceInducers.GravityController
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

object VSGravityManager {
    val __gravities = mutableMapOf<DimensionId, Vector3d>()
    init {
        ServerLevelHolder.server!!.allLevels.forEach { __gravities[it.dimensionId] = Vector3d(0, -10, 0) }
        loadState()
        VSEvents.shipLoadEvent.on { (ship) ->
            val ship = ServerLevelHolder.server!!.shipObjectWorld.loadedShips.getById(ship.id) ?: return@on
            GravityController.getOrCreate(ship)
        }
    }
    //TODO this is dumb
    private fun saveState() {
        val str = __gravities.map { "${it.key} ${it.value.x} ${it.value.y} ${it.value.z}" }.joinToString()
        VMConfig.SERVER.DIMENSION_GRAVITY_VALUES = str
    }

    private fun loadState() {
        val str = VMConfig.SERVER.DIMENSION_GRAVITY_VALUES

        str.split(",").forEach {
            try {
                val items = it.split(" ")
                __gravities[items[0]] = Vector3d(items[1].toDouble(), items[2].toDouble(), items[3].toDouble())
            } catch (_: Exception) {}
        }
    }

    fun setGravity(level: ServerLevel, gravity: Vector3d) {
        __gravities.getOrPut(level.dimensionId) {Vector3d(gravity)}.set(gravity.x, gravity.y, gravity.z)
        saveState()
    }

    fun setGravity(id: DimensionId, gravity: Vector3d) {
        __gravities.getOrPut(id) {Vector3d(gravity)}.set(gravity.x, gravity.y, gravity.z)
        saveState()
    }

    fun getDimensionGravityMutableReference(id: DimensionId): Vector3d {
        return __gravities.getOrElse(id) {
            val default = Vector3d(0, -10, 0)
            __gravities[id] = default
            saveState()
            default
        }
    }
}