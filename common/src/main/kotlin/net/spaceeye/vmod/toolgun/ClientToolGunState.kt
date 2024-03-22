package net.spaceeye.vmod.toolgun

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.event.EventResult
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.toolgun.modes.ToolgunModes.modes
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.ClientClosable
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val GUI_MENU_OPEN_OR_CLOSE = register(
        KeyMapping(
        "key.vmod.gui_open_or_close",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_TAB,
        "vmod.keymappings_name"
    ))

    val TOOLGUN_REMOVE_TOP_CONSTRAINT = register(
        KeyMapping(
            "key.vmod.remove_top_constraint",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_RESET_KEY = register(
        KeyMapping(
            "key.vmod.reset_constraint_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "vmod.keymappings_name"
        )
    )

    fun register(keyMapping: KeyMapping): KeyMapping {
        KeyMappingRegistry.register(keyMapping)
        return keyMapping
    }

    var currentMode: BaseMode? = null

    fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): EventResult {
        val eventResult = if (currentMode == null) { EventResult.pass() } else { currentMode!!.handleKeyEvent(keyCode, scanCode, action, modifiers) }
        if (eventResult != EventResult.pass()) { return eventResult }

        if (action == GLFW.GLFW_PRESS && TOOLGUN_REMOVE_TOP_CONSTRAINT.matches(keyCode, scanCode)) {
            ServerToolGunState.c2sRequestRemoveLastConstraint.sendToServer(ServerToolGunState.C2SRequestRemoveLastConstraintPacket())
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {return EventResult.interruptFalse()}
        return currentMode!!.handleMouseButtonEvent(button, action, modifiers)
    }

    fun handleMouseScrollEvent(amount: Double): EventResult {
        if (currentMode == null) { return EventResult.pass() }
        return currentMode!!.handleMouseScrollEvent(amount)
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