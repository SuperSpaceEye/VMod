package net.spaceeye.vmod.toolgun

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.EventResult
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.ToolgunGUI
import net.spaceeye.vmod.toolgun.modes.ToolgunModes.modes
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.utils.ClientClosable
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    var currentMode: BaseMode? = null

    private var _refreshHUD = true
    fun refreshHUD() { _refreshHUD = true }

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

    private fun register(keyMapping: KeyMapping): KeyMapping {
        KeyMappingRegistry.register(keyMapping)
        return keyMapping
    }

    //TODO events should also have try catches so that it doesn't ever crash
    internal fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): EventResult {
        val eventResult = if (currentMode == null) { EventResult.pass() } else { currentMode!!.handleKeyEvent(keyCode, scanCode, action, modifiers) }
        if (eventResult != EventResult.pass()) { return eventResult }

        if (action == GLFW.GLFW_PRESS && TOOLGUN_REMOVE_TOP_CONSTRAINT.matches(keyCode, scanCode)) {
            ServerToolGunState.c2sRequestRemoveLastConstraint.sendToServer(ServerToolGunState.C2SRequestRemoveLastConstraintPacket())
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    internal fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {return EventResult.interruptFalse()}
        return currentMode!!.handleMouseButtonEvent(button, action, modifiers)
    }

    internal fun handleMouseScrollEvent(amount: Double): EventResult {
        if (currentMode == null) { return EventResult.pass() }
        return currentMode!!.handleMouseScrollEvent(amount)
    }

    private var screenGui: ScreenWindow? = null

    internal fun onRenderHUD(stack: PoseStack, delta: Float) {
        val currentMode = currentMode ?: return

        val screenGui = screenGui ?: run {
            val temp = ScreenWindow()
            val minecraft = Minecraft.getInstance()
            temp.init(minecraft, minecraft.window.guiScaledWidth, minecraft.window.guiScaledHeight)
            screenGui = temp
            temp
        }

        if (_refreshHUD) {
            screenGui.screenContainer.clearChildren()
            currentMode.makeHUD(screenGui.screenContainer)
            _refreshHUD = false
        }

        screenGui.render(stack, 0, 0, delta)
    }

    internal lateinit var gui: ToolgunGUI

    internal fun guiIsOpened() = Minecraft.getInstance().screen == gui
    internal fun otherGuiIsOpened() = Minecraft.getInstance().screen != null && Minecraft.getInstance().screen != gui

    internal fun init() {
        gui = ToolgunGUI()
        gui.makeScrollComponents(modes)
    }

    override fun close() {
        currentMode = null
    }
}