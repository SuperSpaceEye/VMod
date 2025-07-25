package net.spaceeye.vmod.toolgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import gg.essential.elementa.components.UIBlock
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.toolgun.gui.ClientSettingsGUI
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.gui.ServerSettingsGUI
import net.spaceeye.vmod.toolgun.gui.SettingPresets
import net.spaceeye.vmod.toolgun.gui.ToolgunGUI
import net.spaceeye.vmod.toolgun.gui.ToolgunWindow
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.state.VEntityChangerGui
import net.spaceeye.vmod.translate.CLIENT_SETTINGS
import net.spaceeye.vmod.translate.MAIN
import net.spaceeye.vmod.translate.SERVER_SETTINGS
import net.spaceeye.vmod.translate.SETTINGS_PRESETS
import net.spaceeye.vmod.utils.ClientClosable
import net.spaceeye.vmod.utils.EmptyPacket
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val modes = ToolgunModes.asList().map { it.get() }.map { it.init(BaseNetworking.EnvType.Client); it }

    var currentMode: BaseMode? = null
        set(value) {
            field?.onCloseMode()
            field = value
            field?.onOpenMode()
        }

    private val externalWindows = mutableListOf<Pair<TranslatableComponent, (UIBlock) -> ToolgunWindow>>()

    fun addWindow(name: TranslatableComponent, windowConstructor: (UIBlock) -> ToolgunWindow) {
        externalWindows.add(name to windowConstructor)
    }

    init {
        addWindow(MAIN) { ToolgunGUI(it) }
        addWindow(CLIENT_SETTINGS) {ClientSettingsGUI(it)}
        addWindow(SERVER_SETTINGS) {ServerSettingsGUI(it)}
        addWindow(SETTINGS_PRESETS) {SettingPresets(it)}

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
                if (it != Minecraft.getInstance().player) {return@register}
                currentMode = null
                refreshHUD()
            }

            PersistentEvents.keyPress.on {
                    (keyCode, scanCode, action, modifiers), _ ->
                if (!playerIsUsingToolgun()) {return@on false}
                if (otherGuiIsOpened()) {return@on false}

                val guiIsOpened = guiIsOpened()
                val isPressed = action == GLFW.GLFW_PRESS

                // we do it like this because we need for toolgun to handle keys first to prevent
                // user from opening menu or smth in the middle of using some mode
                if (!guiIsOpened) {
                    val cancel = handleKeyEvent(keyCode, scanCode, action, modifiers)
                    if (cancel) {return@on true}

                    if (isPressed && GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode)) {
                        openGUI()
                        return@on true
                    }
                }

                if (!guiIsOpened && isPressed && TOOLGUN_TOGGLE_HUD_KEY.matches(keyCode, scanCode)) {
                    ScreenWindow.renderHud = !ScreenWindow.renderHud
                    return@on true
                }

                if (guiIsOpened && isPressed && (GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
                    closeGUI()
                    return@on true
                }

                return@on false
            }

            ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
                    client, button, action, mods ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                return@register handleMouseButtonEvent(button, action, mods)
            }

            ClientRawInputEvent.MOUSE_SCROLLED.register {
                    client, amount ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                return@register handleMouseScrollEvent(amount)
            }

            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
                init()
            }

            //todo why did i do this
//            var inited = false
//            ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register {
//                if (inited) { return@register }
//                init()
//                inited = true
//            }
//            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
//                if (it != Minecraft.getInstance().player || it == null) {return@register}
//                inited = false
//            }
        }}
    }

    fun refreshHUD() { ScreenWindow.screen?.getExtensionOfType<HUDAddition>()?.refreshHUD() }

    val GUI_MENU_OPEN_OR_CLOSE = ToolgunKeybinds.GUI_MENU_OPEN_OR_CLOSE
    val TOOLGUN_REMOVE_TOP_VENTITY = ToolgunKeybinds.TOOLGUN_REMOVE_TOP_VENTITY
    val TOOLGUN_RESET_KEY = ToolgunKeybinds.TOOLGUN_RESET_KEY
    val TOOLGUN_TOGGLE_HUD_KEY = ToolgunKeybinds.TOOLGUN_TOGGLE_HUD_KEY
    val TOOLGUN_CHANGE_PRESET_KEY = ToolgunKeybinds.TOOLGUN_CHANGE_PRESET_KEY

    fun openGUI() {
        gui.onGUIOpen()
        Minecraft.getInstance().setScreen(gui)
    }

    fun closeGUI() {
        Minecraft.getInstance().setScreen(null)
    }

    fun closeWithError(error: String) {
        closeGUI()
        ScreenWindow.addHUDError(error)
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
        if (currentMode == null) {
            return when(button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> EventResult.interruptFalse()
                else -> EventResult.pass()
            }
        }
        return currentMode!!.onMouseButtonEvent(button, action, modifiers)
    }

    internal fun handleMouseScrollEvent(amount: Double): EventResult {
        if (currentMode == null) { return EventResult.pass() }
        return currentMode!!.onMouseScrollEvent(amount)
    }

    private lateinit var gui: MainToolgunGUIWindow

    //TODO redo this
    internal fun guiIsOpened() = Minecraft.getInstance().screen.let { it is MainToolgunGUIWindow || it is VEntityChangerGui }
    internal fun otherGuiIsOpened() = Minecraft.getInstance().screen.let { it != null && it !is MainToolgunGUIWindow && it !is VEntityChangerGui }
    fun playerIsUsingToolgun(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        return player.mainHandItem.item == VMItems.TOOLGUN.get().asItem()
    }

    internal fun init() {
        gui = MainToolgunGUIWindow()
        externalWindows.forEach { gui.addWindow(it.first, it.second) }
        gui.initGUI()
    }

    override fun close() {
        currentMode = null
    }
}