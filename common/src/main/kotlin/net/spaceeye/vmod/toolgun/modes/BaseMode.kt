package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState.idWithConnc
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.toolgun.modes.util.serverTryActivate
import net.spaceeye.vmod.utils.RaycastFunctions

interface GUIBuilder {
    val itemName: Component
    fun makeGUISettings(parentWindow : UIContainer)
}

interface HUDBuilder {
    fun makeHUD(screen: UIContainer) {}
}

interface ClientEventsHandler {
    fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult { return EventResult.pass() }
    fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult { return EventResult.pass() }
    fun onMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }

    fun onOpenMode() {}
    fun onCloseMode() {}
}

interface MSerializable: AutoSerializable {
    fun serverSideVerifyLimits() {
        getSerializableItems().forEach { it.setValue(null, null, it.verification(it.it)) }
    }
}

interface BaseMode : MSerializable, GUIBuilder, HUDBuilder, ClientEventsHandler {
    fun resetState() {}

    fun init(type: BaseNetworking.EnvType) {}

    fun refreshHUD() { ClientToolGunState.refreshHUD() }

    fun <T: Serializable> register(constructor: () -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor()
        if (!ToolgunModes.initialized) {
            try { NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler()) } catch (e: NoSuchMethodError) { }
        }
        return instance
    }

}

inline fun <reified T: BaseMode> BaseMode.registerConnection(instance: T, name: String, noinline toExecute: (item: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit) = register {
    name idWithConnc {
        object : C2SConnection<T>(it, "toolgun_command") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) =
                serverRaycastAndActivate<T>(context.player, buf, {instance.javaClass.getConstructor().newInstance()}, toExecute)
        }
    }
}

inline fun <reified T: BaseMode> BaseMode.registerConnection(instance: T, name: String, noinline toExecute: (item: T, level: ServerLevel, player: ServerPlayer) -> Unit) = register {
    name idWithConnc {
        object : C2SConnection<T>(name, "toolgun_command") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) =
                serverTryActivate<T>(context.player, buf, {instance.javaClass.getConstructor().newInstance()}, toExecute)
        }
    }
}

abstract class BaseNetworking <T: BaseMode>: NetworkingRegisteringFunctions {
    var clientObj: T? = null
    var serverObj: T? = null

    enum class EnvType { Client, Server }

    fun init(obj: T, type: EnvType) {
        when (type) {
            EnvType.Client -> clientObj = obj
            EnvType.Server -> serverObj = obj
        }
    }
}