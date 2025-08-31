package net.spaceeye.vmod.toolgun.modes.util

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.rendering.Effects
import net.spaceeye.vmod.toolgun.SELOG
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.translate.TOOLGUN_MODE_ACTIVATION_HAS_FAILED
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import java.util.function.Supplier

fun <T: BaseMode> BaseMode.serverTryActivate(
    player: Player,
    buf: FriendlyByteBuf,
    clazz: Class<out T>,
    supplier: Supplier<BaseMode>,
    fn: (item: T, level: ServerLevel, player: ServerPlayer) -> Unit
) = instance.server.verifyPlayerAccessLevel(player as ServerPlayer, clazz as Class<BaseMode>) {
    var serverMode = instance.server.playersStates.getOrPut(player.uuid) { supplier.get() }
    if (!clazz.isInstance(serverMode)) { serverMode = supplier.get(); instance.server.playersStates[player.uuid] = serverMode }

    try {
        //TODO activate only one time?
        serverMode.init(BaseNetworking.EnvType.Server)
        serverMode.deserialize(buf)
        serverMode.serverSideVerifyLimits()

        SessionEvents.serverAfterTick.on { _, unsubscribe ->
            unsubscribe()
            try {
                fn(serverMode as T, player.serverLevel(), player)
            } catch (e: Exception) {
                SELOG("Failed to activate function of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}", player, TOOLGUN_MODE_ACTIVATION_HAS_FAILED)
            }
        }
    } catch (e: Exception) {
        SELOG("Failed to activate function of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}", player, TOOLGUN_MODE_ACTIVATION_HAS_FAILED)
    }
}

fun <T : BaseMode> BaseMode.serverRaycastAndActivate(
    player: Player,
    buf: FriendlyByteBuf,
    clazz: Class<out T>,
    supplier: Supplier<BaseMode>,
    fn: (T, ServerLevel, ServerPlayer, RaycastFunctions.RaycastResult) -> Unit
) = serverTryActivate<T>(player, buf, clazz, supplier) { item: T, level: ServerLevel, player: ServerPlayer ->
    try {
        val result = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(player.lookAngle), Vector3d(player.eyePosition)),
            VMConfig.SERVER.TOOLGUN.MAX_RAYCAST_DISTANCE
        )

        Effects.sendToolgunRayEffect(player, result, VMConfig.SERVER.TOOLGUN.MAX_RAYCAST_DISTANCE)

        fn(item, level, player, result)
    } catch (e: Exception) {
        SELOG("Failed to activate function of ${item.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}", player, TOOLGUN_MODE_ACTIVATION_HAS_FAILED)
    }
}