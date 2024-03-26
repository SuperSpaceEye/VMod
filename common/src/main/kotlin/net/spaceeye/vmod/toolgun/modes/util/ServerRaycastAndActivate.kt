package net.spaceeye.vmod.toolgun.modes.util

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.rendering.Effects
import net.spaceeye.vmod.toolgun.PlayerToolgunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d

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
    val result = RaycastFunctions.raycast(
        level,
        RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
        100.0
    )

    Effects.sendToolgunRayEffect(player, result, 100.0)

    fn(serverMode.mode as T, level, player, result)
    } catch (e: Exception) {
        ELOG("Failed to activate function ${fn.javaClass.name} of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}")
    }
}