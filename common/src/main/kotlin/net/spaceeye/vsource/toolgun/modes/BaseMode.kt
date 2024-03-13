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
import net.spaceeye.vsource.toolgun.PlayerToolgunState
import net.spaceeye.vsource.toolgun.ServerToolGunState
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.Vector3d

interface GUIItem {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow : UIBlock)
}

interface BaseMode : Serializable, GUIItem {
    fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int) : EventResult
    fun handleMouseButtonEvent(button: Int, action: Int, mods: Int) : EventResult
    fun serverSideVerifyLimits()

     fun <T: Serializable> register(constructor: () -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor()
        if (!ToolgunModes.initialized) {
            try { NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler()) } catch (e: NoSuchMethodError) { }
        }
        return instance
    }
}

inline fun <reified T : BaseMode> BaseMode.serverRaycastAndActivate(
    player: Player,
    buf: FriendlyByteBuf,
    constructor: () -> BaseMode,
    noinline fn: (item: T, ServerLevel, Player, RaycastFunctions.RaycastResult) -> Unit) {
    val level = player.level as ServerLevel

    var serverMode = ServerToolGunState.playersStates.getOrPut(player.uuid) { PlayerToolgunState(constructor()) }
    if (serverMode.mode !is T) { serverMode = PlayerToolgunState(constructor()); ServerToolGunState.playersStates[player.uuid] = serverMode }

    try {
    serverMode.mode.deserialize(buf)
    serverMode.mode.serverSideVerifyLimits()

    //TODO add maxDistance to config
    val result = RaycastFunctions.raycast(level, RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)), 100.0)

    sendToolgunRayEffect(player, result, 100.0)

    fn(serverMode.mode as T, level, player, result)
    } catch (e: Exception) {
        ELOG("Failed to activate function ${fn.javaClass.name} of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}")
    }
}