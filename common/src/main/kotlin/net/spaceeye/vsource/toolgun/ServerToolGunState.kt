package net.spaceeye.vsource.toolgun

import net.minecraft.world.entity.player.Player
import net.spaceeye.vsource.toolgun.modes.BaseMode
import net.spaceeye.vsource.utils.ServerClosable
import java.util.concurrent.ConcurrentHashMap

object ServerToolGunState: ServerClosable() {
    val playerStates = ConcurrentHashMap<Player, BaseMode>()

    override fun close() {
        playerStates.clear()
    }
}