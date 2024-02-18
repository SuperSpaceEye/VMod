package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.networking.Serializable

interface GUIItem {
    val itemName: String
    fun makeGUISettings(parentWindow : UIBlock)
}

interface BaseMode : Serializable, GUIItem {
    fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int) : EventResult
    fun handleMouseButtonEvent(button: Int, action: Int, mods: Int) : EventResult

     fun <T: Serializable> register(constructor: () -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor()
        try {
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}