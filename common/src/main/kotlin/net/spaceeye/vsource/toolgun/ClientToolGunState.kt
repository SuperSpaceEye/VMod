package net.spaceeye.vsource.toolgun

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.event.EventResult
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.spaceeye.vsource.toolgun.modes.ToolgunModes.modes
import net.spaceeye.vsource.toolgun.modes.BaseMode
import net.spaceeye.vsource.utils.ClientClosable

object ClientToolGunState : ClientClosable() {
    val GUI_MENU_OPEN_OR_CLOSE = register(
        KeyMapping(
        "key.vsource.gui_open_or_close",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_TAB,
        "vsourse.keymappings_name"
    ))

    fun register(keyMapping: KeyMapping): KeyMapping {
        KeyMappingRegistry.register(keyMapping)
        return keyMapping
    }

    var currentMode: BaseMode? = null

    fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): EventResult {
        if (currentMode == null) {return EventResult.pass()}
        return currentMode!!.handleKeyEvent(keyCode, scanCode, action, modifiers)
    }

    fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {return EventResult.interruptFalse()}
        return currentMode!!.handleMouseButtonEvent(button, action, modifiers)
    }

    lateinit var gui: ToolgunGUI

    fun guiIsOpened() = Minecraft.getInstance().screen == gui
    fun otherGuiIsOpened() = Minecraft.getInstance().screen != null && Minecraft.getInstance().screen != gui

    fun init() {
        gui = ToolgunGUI()
        gui.makeScrollComponents(modes)
    }

    override fun close() {
        currentMode = null
    }
}