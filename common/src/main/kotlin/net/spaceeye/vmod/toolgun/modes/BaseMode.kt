package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.toolgun.ClientToolGunState

interface GUIBuilder {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow: UIContainer)
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