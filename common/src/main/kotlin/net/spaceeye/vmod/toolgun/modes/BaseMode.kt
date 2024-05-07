package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.Serializable

interface GUIBuilder {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow : UIBlock)
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

abstract class BaseNetworking <T: BaseMode> {
    var clientObj: T? = null
    var serverObj: T? = null

    enum class EnvType { Client, Server }

    fun init(obj: T, type: EnvType) {
        when (type) {
            EnvType.Client -> clientObj = obj
            EnvType.Server -> serverObj = obj
        }
    }

    infix fun <TT: Serializable> String.idWithConns(constructor: (String) -> S2CConnection<TT>): S2CConnection<TT> {
        val instance = constructor(this)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }

    infix fun <TT: Serializable> String.idWithConnc(constructor: (String) -> C2SConnection<TT>): C2SConnection<TT> {
        val instance = constructor(this)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}