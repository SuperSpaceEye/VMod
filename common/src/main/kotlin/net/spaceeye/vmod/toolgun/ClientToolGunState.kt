package net.spaceeye.vmod.toolgun

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.EventResult
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.utils.ClientClosable
import net.spaceeye.vmod.utils.EmptyPacket
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val modes = ToolgunModes.asList().map { it.get() }.map { it.init(BaseNetworking.EnvType.Client); it }

    private var _currentMode: BaseMode? = null
    var currentMode: BaseMode?
        get() = _currentMode
        set(value) {
            _currentMode?.onCloseMode()
            _currentMode = value
            _currentMode?.onOpenMode()
        }

    fun refreshHUD() { screen?.refreshHUD() }

    val GUI_MENU_OPEN_OR_CLOSE = register(
        KeyMapping(
        "key.vmod.gui_open_or_close",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_TAB,
        "vmod.keymappings_name"
    ))

    val TOOLGUN_REMOVE_TOP_VENTITY = register(
        KeyMapping(
            "key.vmod.remove_top_ventity",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_RESET_KEY = register(
        KeyMapping(
            "key.vmod.reset_ventity_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "vmod.keymappings_name"
        )
    )

    fun openGUI() {
        gui.onGUIOpen()
        Minecraft.getInstance().setScreen(gui)
    }

    fun closeGUI() {
        Minecraft.getInstance().setScreen(null)
    }

    private fun register(keyMapping: KeyMapping): KeyMapping {
        KeyMappingRegistry.register(keyMapping)
        return keyMapping
    }

    internal fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): Boolean {
        val cancel = if (currentMode == null) { false } else { currentMode!!.onKeyEvent(keyCode, scanCode, action, modifiers) }
        if (cancel) { return true }

        if (action == GLFW.GLFW_PRESS && TOOLGUN_REMOVE_TOP_VENTITY.matches(keyCode, scanCode)) {
            ServerToolGunState.c2sRequestRemoveLastVEntity.sendToServer(EmptyPacket())
            return true
        }

        return false
    }

    internal fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {return EventResult.interruptFalse()}
        return currentMode!!.onMouseButtonEvent(button, action, modifiers)
    }

    internal fun handleMouseScrollEvent(amount: Double): EventResult {
        if (currentMode == null) { return EventResult.pass() }
        return currentMode!!.onMouseScrollEvent(amount)
    }

    private var screen: ScreenWindow? = null

    //TODO make a unified way of using those
    internal fun addHUDError(str: String) {
        screen?.addError(str)
    }

    internal fun onRenderHUD(stack: PoseStack, delta: Float) {
        try {
        (screen ?: run {
            val temp = ScreenWindow()
            val minecraft = Minecraft.getInstance()
            temp.init(minecraft, minecraft.window.guiScaledWidth, minecraft.window.guiScaledHeight)
            screen = temp
            temp
        }).onRenderHUD(stack, delta)
        } catch (e: Exception) {
        } catch (e: Error) {}
    }

    private lateinit var gui: MainToolgunGUIWindow

    internal fun guiIsOpened() = Minecraft.getInstance().screen == gui
    internal fun otherGuiIsOpened() = Minecraft.getInstance().screen != null && Minecraft.getInstance().screen != gui

    internal fun init() {
        gui = MainToolgunGUIWindow()
    }

    override fun close() {
        currentMode = null
    }
}