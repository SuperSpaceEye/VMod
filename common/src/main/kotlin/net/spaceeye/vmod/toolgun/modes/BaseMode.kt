package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.NetworkingRegisteringFunctions
import net.spaceeye.vmod.networking.Serializable

interface GUIBuilder {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow: UIContainer)
}

interface ClientRawInputsHandler {
    fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int) : EventResult { return EventResult.pass() }
    fun handleMouseButtonEvent(button: Int, action: Int, mods: Int) : EventResult { return EventResult.pass() }
    fun handleMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }
}

interface MSerializable: Serializable {
    fun serverSideVerifyLimits()
}

interface BaseMode : MSerializable, GUIBuilder, ClientRawInputsHandler {
    fun resetState() {}

    fun init(type: BaseNetworking.EnvType) {}

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