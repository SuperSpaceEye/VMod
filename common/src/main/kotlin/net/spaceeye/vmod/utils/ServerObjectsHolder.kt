package net.spaceeye.vmod.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore
import org.valkyrienskies.mod.common.dimensionId

//this is incredibly stupid lmao
object ServerObjectsHolder {
    //overworld server level
    var overworldServerLevel: ServerLevel? = null
    var server: MinecraftServer? = null
    var shipObjectWorld: ServerShipWorldCore? = null
    fun getLevelById(dimensionId: String): ServerLevel? {
        val server = server ?: return null
        return server.allLevels!!.find { it.dimensionId == dimensionId }
    }
}