package net.spaceeye.vsource.toolgun

import dev.architectury.event.EventResult
import net.minecraft.client.Minecraft
import net.spaceeye.vsource.toolgun.ToolgunModes.modes
import net.spaceeye.vsource.toolgun.modes.BaseMode
import net.spaceeye.vsource.utils.ClientClosable
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val guiOpenKey = GLFW.GLFW_KEY_E

    var currentMode: BaseMode? = null

    fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): EventResult {
        if (currentMode == null) {return EventResult.pass()}
        return currentMode!!.handleKeyEvent(keyCode, scanCode, action, modifiers)
    }

    fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {return EventResult.pass()}
        return currentMode!!.handleMouseButtonEvent(button, action, modifiers)
    }

    lateinit var gui: ToolgunGUI

    fun guiIsOpened() = Minecraft.getInstance().screen == gui

    fun init() {
        gui = ToolgunGUI()
        gui.makeScrollComponents(modes)
    }

    override fun close() {
        currentMode = null
    }
}