package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.modes.util.ToolgunModes

interface GUIBuilder {
    val itemName: TranslatableComponent
    fun makeGUISettings(parentWindow : UIBlock)
}

interface ClientRawInputsHandler {
    fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int) : EventResult { return EventResult.pass() }
    fun handleMouseButtonEvent(button: Int, action: Int, mods: Int) : EventResult { return EventResult.pass() }
    fun handleMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }
}

interface ServerHandler {}

interface ModeSerializable: Serializable {
    fun serverSideVerifyLimits()
}

interface BaseMode : ModeSerializable, GUIBuilder, ClientRawInputsHandler {
     fun <T: Serializable> register(constructor: () -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor()
        if (!ToolgunModes.initialized) {
            try { NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler()) } catch (e: NoSuchMethodError) { }
        }
        return instance
    }
}
