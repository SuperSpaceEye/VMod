package net.spaceeye.vmod.toolgun

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.networking.SerializableItem.registerSerializationEnum
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.ToolgunModes.getPermission
import net.spaceeye.vmod.translate.REMOVED
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerToolgunState(var mode: BaseMode)

fun sendHUDErrorToPlayer(player: ServerPlayer, error: String) {
    ServerToolGunState.s2cErrorHappened.sendToClient(player, ServerToolGunState.S2CErrorHappened(error))
}

fun sendHUDErrorToOperators(error: String) {
    ServerLevelHolder.server!!.playerList.players.forEach {
        if (!it.hasPermissions(4)) {return@forEach}
        sendHUDErrorToPlayer(it, error)
    }
}

object ServerToolGunState: ServerClosable() {
    val playersStates = ConcurrentHashMap<UUID, PlayerToolgunState>()
    val playersConstraintsStack = ConcurrentHashMap<UUID, MutableList<ManagedConstraintId>>()

    init {
        // it needs to initialize all c2s and s2c receivers
        ToolgunModes.asList().forEach { it.get().init(BaseNetworking.EnvType.Server) }

        registerSerializationEnum(AccessTo::class)
    }

    @JvmStatic fun playerHasAccess(player: ServerPlayer): Boolean {
        return      player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_TOOLGUN_PERMISSION_LEVEL)
                && !ToolgunPermissionManager.getDisallowedPlayers().contains(player.uuid)
                ||  ToolgunPermissionManager.getAllowedPlayers().contains(player.uuid)
    }

    @JvmStatic inline fun verifyPlayerAccessLevel(player: ServerPlayer, clazz: Class<BaseMode>, fn: () -> Unit) {
        if (!PlayerAccessManager.hasPermission(player, clazz.getPermission())) {
            s2cToolgunUsageRejected.sendToClient(player, EmptyPacket())
            return
        }
        fn()
    }

    @JvmStatic inline fun verifyPlayerAccessLevel(player: ServerPlayer, permission: String, fn: () -> Unit) {
        if (!PlayerAccessManager.hasPermission(player, permission)) {
            s2cToolgunUsageRejected.sendToClient(player, EmptyPacket())
            return
        }
        fn()
    }

    override fun close() {
        playersStates.clear()
        playersConstraintsStack.clear()
    }
    val s2cToolgunUsageRejected = regS2C<EmptyPacket>("toolgun_usage_rejected", "server_toolgun") {
        ClientToolGunState.currentMode?.resetState()
        ClientToolGunState.addHUDError("You don't have the permission to use toolgun")
    }

    val c2sRequestRemoveLastConstraint = regC2S<EmptyPacket>("request_remove_last_constraint", "server_toolgun",
        {PlayerAccessManager.hasPermission(it, "request_remove_last_constraint")},
        {s2cToolgunUsageRejected.sendToClient(it, EmptyPacket())}
        ) { pkt, player->
        val stack = playersConstraintsStack[player.uuid] ?: return@regC2S
        var item: ManagedConstraintId = stack.removeLastOrNull() ?: return@regC2S

        val level = player.level as ServerLevel

        RandomEvents.serverOnTick.on {
                _, unsubscribe ->
            unsubscribe()
            // if constraint wasn't already removed, then remove it
            while (true) {
                if (level.removeManagedConstraint(item)) {
                    break
                } else {
                    item = stack.removeLastOrNull() ?: return@on
                }
            }

            player.sendMessage(REMOVED, player.uuid)
        }
    }

    data class S2CErrorHappened(var errorStr: String): AutoSerializable

    val s2cErrorHappened = regS2C<S2CErrorHappened>("error_happened", "server_toolgun") {(errorStr) ->
        ClientToolGunState.addHUDError(errorStr)
    }

    val s2cTooglunWasReset = regS2C<EmptyPacket>("toolgun_was_reset", "server_toolgun") {
        ClientToolGunState.currentMode?.resetState()
        ClientToolGunState.currentMode?.refreshHUD()
    }

    val c2sToolgunWasReset = regC2S<EmptyPacket>("toolgun_was_reset", "server_toolgun") {pkt, player ->
        playersStates[player.uuid]?.mode?.resetState()
    }

    enum class AccessTo {
        NormalToolgunUsage,
        ServerSettings
    }

    data class C2SAskIfIHaveAccess(var accessTo: AccessTo, var callbackId: UUID): AutoSerializable

    val callbacks = mutableMapOf<UUID, (Boolean) -> Unit>()

    fun checkIfIHaveAccess(accessTo: AccessTo, callback: (Boolean) -> Unit) {
        val askUUID = UUID.randomUUID()
        callbacks[askUUID] = callback
        c2sAskIfIHaveAccess.sendToServer(C2SAskIfIHaveAccess(accessTo, askUUID))
    }

    private val c2sAskIfIHaveAccess = regC2S<C2SAskIfIHaveAccess>("ask_if_i_have_access", "server_toolgun") {pkt, player ->
        val generalAccess = playerHasAccess(player)

        if (!generalAccess) { return@regC2S s2cResponseToAccessRequest.sendToClient(player, S2CResponseToAccessRequest(pkt.accessTo, pkt.callbackId, false)) }

        val hasAccess = when (pkt.accessTo) {
            AccessTo.NormalToolgunUsage -> generalAccess
            AccessTo.ServerSettings -> player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_CHANGING_SERVER_SETTINGS_LEVEL)
        }

        s2cResponseToAccessRequest.sendToClient(player, S2CResponseToAccessRequest(pkt.accessTo, pkt.callbackId, hasAccess))
    }

    data class S2CResponseToAccessRequest(var accessTo: AccessTo, val callbackId: UUID, var response: Boolean): AutoSerializable

    val s2cResponseToAccessRequest = regS2C<S2CResponseToAccessRequest>("response_to_access_request", "server_toolgun") {
        callbacks[it.callbackId]?.invoke(it.response)
    }
}