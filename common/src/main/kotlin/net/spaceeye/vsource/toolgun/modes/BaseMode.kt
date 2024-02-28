package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vsource.ELOG
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.networking.Serializable
import net.spaceeye.vsource.rendering.Effects.sendToolgunRayEffect
import net.spaceeye.vsource.toolgun.ServerToolGunState
import net.spaceeye.vsource.utils.RaycastFunctions

interface GUIItem {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow : UIBlock)
}

interface BaseMode : Serializable, GUIItem {
    fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int) : EventResult
    fun handleMouseButtonEvent(button: Int, action: Int, mods: Int) : EventResult

     fun <T: Serializable> register(constructor: () -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor()
        if (!ToolgunModes.initialized) {
            try { NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler()) } catch (e: NoSuchMethodError) { }
        }
        return instance
    }
}

inline fun <reified T : BaseMode> BaseMode.serverRaycastAndActivate(player: Player, buf: FriendlyByteBuf, constructor: () -> BaseMode, noinline fn: (ServerLevel, Player, RaycastFunctions.RaycastResult) -> Unit) {
    val level = player.level as ServerLevel

    var serverMode = ServerToolGunState.playerStates.getOrPut(player, constructor)
    if (serverMode !is T) { serverMode = constructor(); ServerToolGunState.playerStates[player] = serverMode }

    try {
    serverMode.deserialize(buf)

    //TODO add maxDistance to config
    val result = RaycastFunctions.raycast(level, player, 100.0)

    sendToolgunRayEffect(player, result, 100.0)

    fn(level, player, result)
    } catch (e: Exception) {
        ELOG("Failed to activate function ${fn.javaClass.name} of ${serverMode.javaClass.simpleName} called by player ${player.name.contents}")
    }
}