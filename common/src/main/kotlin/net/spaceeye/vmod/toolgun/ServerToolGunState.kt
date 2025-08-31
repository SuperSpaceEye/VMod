package net.spaceeye.vmod.toolgun

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.vEntityManaging.VEntityId
import net.spaceeye.vmod.vEntityManaging.removeVEntity
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ToolgunModes.getPermission
import net.spaceeye.vmod.translate.REMOVED
import net.spaceeye.vmod.translate.YOU_DONT_HAVE_PERMISSION_TO_USE_TOOLGUN
import net.spaceeye.vmod.translate.getTranslationKey
import net.spaceeye.vmod.translate.translate
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.ServerClosable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

fun SELOG(s: String, player: ServerPlayer, toSend: String, translatable: Boolean = true) {
    ELOG(s)
    VMToolgun.server.sendErrorTo(player, toSend, translatable, true)
}
fun SELOG(s: String, player: ServerPlayer?, toSend: Component) = player?.also { SELOG(s, player, toSend.getTranslationKey(), true) } ?: ELOG(s)

open class ServerToolGunState(var instance: ToolgunInstance): ServerClosable() {
    open val playersStates = ConcurrentHashMap<UUID, BaseMode>()
    open val playersVEntitiesStack = ConcurrentHashMap<UUID, MutableList<VEntityId>>()

    data class S2CErrorHappened(var errorStr: String, var translatable: Boolean = true, var closeGUI: Boolean = false): AutoSerializable

    open val s2cErrorHappened = regS2C<S2CErrorHappened>(instance.modId, "error_happened_reset_toolgun", "server_toolgun") { (errorStr, translate, closeGUI) ->
        SessionEvents.clientOnTick.on { _, unsub -> unsub.invoke()
            instance.client.currentMode?.resetState()
            instance.client.currentMode?.refreshHUD()
            instance.client.closeWithError(if (translate) errorStr.translate() else errorStr)
        }
    }

    open val s2cToolgunWasReset = regS2C<EmptyPacket>(instance.modId, "toolgun_was_reset", "server_toolgun") {
        SessionEvents.clientOnTick.on { _, unsub -> unsub.invoke()
            instance.client.currentMode?.resetState()
            instance.client.currentMode?.refreshHUD()
        }
    }

    open val c2sToolgunWasReset = regC2S<EmptyPacket>(instance.modId, "toolgun_was_reset", "server_toolgun") {pkt, player ->
        playersStates[player.uuid]?.resetState()
    }

    fun sendErrorTo(player: ServerPlayer, errorStr: String, translatable: Boolean = true, closeGUI: Boolean = false) = s2cErrorHappened.sendToClient(player, S2CErrorHappened(errorStr, translatable, closeGUI))
    fun sendErrorTo(player: ServerPlayer, errorStr: Component, closeGUI: Boolean = false) = s2cErrorHappened.sendToClient(player, S2CErrorHappened(errorStr.getTranslationKey(), true, closeGUI))

    init {
        instance.server = this
        // it needs to initialize all c2s and s2c receivers
        instance.modeTypes.asList().forEach { it.get().also { it.instance = instance }.init(BaseNetworking.EnvType.Server) }
    }

    open fun playerHasAccess(player: ServerPlayer): Boolean {
        return player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_TOOLGUN_PERMISSION_LEVEL)
    }

    open fun verifyPlayerAccessLevel(player: ServerPlayer, clazz: Class<BaseMode>, fn: () -> Unit) {
        verifyPlayerAccessLevel(player, clazz.getPermission(), fn)
    }

    open fun verifyPlayerAccessLevel(player: ServerPlayer, permission: String, fn: () -> Unit) {
        if (!PlayerAccessManager.hasPermission(player, permission)) {
            s2cErrorHappened.sendToClient(player, S2CErrorHappened(YOU_DONT_HAVE_PERMISSION_TO_USE_TOOLGUN.getTranslationKey()))
            return
        }
        fn()
    }

    open fun playerHasPermission(player: ServerPlayer, clazz: Class<BaseMode>): Boolean = PlayerAccessManager.hasPermission(player, clazz.getPermission())

    override fun close() {
        playersStates.clear()
        playersVEntitiesStack.clear()
    }

    open val c2sRequestRemoveLastVEntity = regC2S<EmptyPacket>(instance.modId, "request_remove_last_ventity", "server_toolgun",
        { pkt, player -> PlayerAccessManager.hasPermission(player, "request_remove_last_ventity")},
        { pkt, player -> s2cErrorHappened.sendToClient(player, S2CErrorHappened(YOU_DONT_HAVE_PERMISSION_TO_USE_TOOLGUN.getTranslationKey()))}
        ) { pkt, player->
        val stack = playersVEntitiesStack[player.uuid] ?: return@regC2S
        var item: VEntityId = stack.removeLastOrNull() ?: return@regC2S

        val level = player.serverLevel() as ServerLevel

        SessionEvents.serverOnTick.on {
                _, unsubscribe ->
            unsubscribe()
            // if VEntity wasn't already removed, then remove it
            while (true) {
                if (level.removeVEntity(item)) {
                    break
                } else {
                    item = stack.removeLastOrNull() ?: return@on
                }
            }

            player.sendSystemMessage(REMOVED)
        }
    }

    //TODO this is dumb, redo
    enum class AccessTo {
        NormalToolgunUsage,
        ServerSettings
    }

    data class C2SAskIfIHaveAccess(var accessTo: AccessTo, var callbackId: UUID): AutoSerializable

    protected open val callbacks = mutableMapOf<UUID, (Boolean) -> Unit>()

    open fun checkIfIHaveAccess(accessTo: AccessTo, callback: (Boolean) -> Unit) {
        val askUUID = UUID.randomUUID()
        callbacks[askUUID] = callback
        c2sAskIfIHaveAccess.sendToServer(C2SAskIfIHaveAccess(accessTo, askUUID))
    }

    protected open val c2sAskIfIHaveAccess = regC2S<C2SAskIfIHaveAccess>(instance.modId, "ask_if_i_have_access", "server_toolgun") {pkt, player ->
        val generalAccess = playerHasAccess(player)

        if (!generalAccess) { return@regC2S s2cResponseToAccessRequest.sendToClient(player, S2CResponseToAccessRequest(pkt.accessTo, pkt.callbackId, false)) }

        val hasAccess = when (pkt.accessTo) {
            AccessTo.NormalToolgunUsage -> generalAccess
            AccessTo.ServerSettings -> player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_CHANGING_SERVER_SETTINGS_LEVEL)
        }

        s2cResponseToAccessRequest.sendToClient(player, S2CResponseToAccessRequest(pkt.accessTo, pkt.callbackId, hasAccess))
    }

    data class S2CResponseToAccessRequest(var accessTo: AccessTo, val callbackId: UUID, var response: Boolean): AutoSerializable

    protected open val s2cResponseToAccessRequest = regS2C<S2CResponseToAccessRequest>(instance.modId, "response_to_access_request", "server_toolgun") {
        callbacks[it.callbackId]?.invoke(it.response)
    }
}