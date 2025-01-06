package net.spaceeye.vmod.toolgun.modes.util

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.rendering.Effects
import net.spaceeye.vmod.toolgun.PlayerToolgunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState.verifyPlayerAccessLevel
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.rotateVecByQuat
import net.spaceeye.vmod.utils.vs.transformDirectionShipToWorld
import org.joml.primitives.AABBd
import org.valkyrienskies.mod.common.getShipsIntersecting
import java.util.function.Supplier

fun <T: BaseMode> BaseMode.serverTryActivate(
    player: Player,
    buf: FriendlyByteBuf,
    clazz: Class<out T>,
    supplier: Supplier<BaseMode>,
    fn: (item: T, level: ServerLevel, player: ServerPlayer) -> Unit
) = verifyPlayerAccessLevel(player as ServerPlayer, clazz as Class<BaseMode>) {
    var serverMode = ServerToolGunState.playersStates.getOrPut(player.uuid) { PlayerToolgunState(supplier.get()) }
    if (!clazz.isInstance(serverMode.mode)) { serverMode = PlayerToolgunState(supplier.get()); ServerToolGunState.playersStates[player.uuid] = serverMode }

    try {
        serverMode.mode.init(BaseNetworking.EnvType.Server)
        serverMode.mode.deserialize(buf)
        serverMode.mode.serverSideVerifyLimits()

        RandomEvents.serverAfterTick.on { _, unsubscribe ->
            unsubscribe()
            try {
                fn(serverMode.mode as T, player.level as ServerLevel, player)
            } catch (e: Exception) {
                ELOG("Failed to activate function of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}")
            }
        }
    } catch (e: Exception) {
        ELOG("Failed to activate function of ${serverMode.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}")
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
        val ship = level.getShipsIntersecting(
            AABBd(
                player.position().x - 0.5, player.position().y - 0.5, player.position().z - 0.5,
                player.position().x + 0.5, player.position().y + 0.5, player.position().z + 0.5
            )).lastOrNull()

//        val lookAngle = ship?.let {
//            val rotation = it.transform.rotation
//            Vector3d(player.lookAngle)
////            Vector3d(rotateVecByQuat(Vector3d(player.lookAngle).toJomlVector3d(), rotation))
////            transformDirectionShipToWorld(ship, Vector3d(player.lookAngle))
////            Vector3d(player.lookAngle)
//        } ?: Vector3d(player.lookAngle)

        val lookAngle = player.lookAngle

        ELOG("Look Angle ${Vector3d(player.lookAngle)}")
//        ELOG("           ${lookAngle}")
        ELOG("Euler ${player.xRot} ${player.yRot}")

        val result = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(Vector3d(lookAngle), Vector3d(player.eyePosition)),
            VMConfig.SERVER.TOOLGUN.MAX_RAYCAST_DISTANCE
        )

        Effects.sendToolgunRayEffect(player, result, VMConfig.SERVER.TOOLGUN.MAX_RAYCAST_DISTANCE)

        fn(item, level, player, result)
    } catch (e: Exception) {
        ELOG("Failed to activate function of ${item.javaClass.simpleName} called by player ${player.name.contents} because of \n${e.stackTraceToString()}")
    }
}