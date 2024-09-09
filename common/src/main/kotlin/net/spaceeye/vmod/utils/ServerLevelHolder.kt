package net.spaceeye.vmod.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld

//this is incredibly stupid lmao
object ServerLevelHolder {
    //overworld server level
    var overworldServerLevel: ServerLevel? = null
    var server: MinecraftServer? = null
    var shipObjectWorld: ShipObjectServerWorld? = null
}