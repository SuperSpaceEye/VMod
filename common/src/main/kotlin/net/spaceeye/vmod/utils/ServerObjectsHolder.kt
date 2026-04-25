package net.spaceeye.vmod.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.mod.common.dimensionId
import java.lang.ref.WeakReference

//this is incredibly stupid lmao
object ServerObjectsHolder {
    private var _overworldServerLevel: WeakReference<ServerLevel>? = null
    private var _server: WeakReference<MinecraftServer>? = null
    private var _shipObjectWorld: WeakReference<VsiServerShipWorld>? = null

    //overworld server level
    var overworldServerLevel: ServerLevel?
        get() = _overworldServerLevel?.get()
        set(value) {_overworldServerLevel = WeakReference(value)}
    var server: MinecraftServer?
        get() = _server?.get()
        set(value) {_server = WeakReference(value)}
    var shipObjectWorld: VsiServerShipWorld?
        get() = _shipObjectWorld?.get()
        set(value) {_shipObjectWorld = WeakReference(value)}
    fun getLevelById(dimensionId: String): ServerLevel? {
        val server = server ?: return null
        return server.allLevels!!.find { it.dimensionId == dimensionId }
    }
}