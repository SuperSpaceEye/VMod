package net.spaceeye.vsource.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel

//this is incredibly stupid lmao
object ServerLevelHolder {
    //overworld server level
    var serverLevel: ServerLevel? = null
    var server: MinecraftServer? = null
}