package net.spaceeye.vmod.toolgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import gg.essential.elementa.components.UIBlock
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.gui.ToolgunGUI
import net.spaceeye.vmod.toolgun.gui.ToolgunWindow
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.state.VEntityChangerGui
import net.spaceeye.vmod.utils.ClientClosable
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.FakeKProperty
import org.lwjgl.glfw.GLFW

open class ClientToolGunState(
    var instance: ToolgunInstance,
    val GUI_MENU_OPEN_OR_CLOSE     :KeyMapping = ToolgunKeybinds.GUI_MENU_OPEN_OR_CLOSE,
    val TOOLGUN_REMOVE_TOP_VENTITY :KeyMapping = ToolgunKeybinds.TOOLGUN_REMOVE_TOP_VENTITY,
    val TOOLGUN_RESET_KEY          :KeyMapping = ToolgunKeybinds.TOOLGUN_RESET_KEY,
    val TOOLGUN_TOGGLE_HUD_KEY     :KeyMapping = ToolgunKeybinds.TOOLGUN_TOGGLE_HUD_KEY,
    val TOOLGUN_CHANGE_PRESET_KEY  :KeyMapping = ToolgunKeybinds.TOOLGUN_CHANGE_PRESET_KEY,
) : ClientClosable() {
    open val modes = instance.modeTypes.asList().map { it.get().also { it.instance = instance } }.map { it.init(BaseNetworking.EnvType.Client); it }

    open var currentMode: BaseMode? = null
        set(value) {
            field?.onCloseMode()
            field = value
            field?.onOpenMode()
        }

    protected open val externalWindows = mutableListOf<Pair<TranslatableComponent, (UIBlock) -> ToolgunWindow>>()

    open fun addWindow(name: TranslatableComponent, windowConstructor: (UIBlock) -> ToolgunWindow) {
        externalWindows.add(name to windowConstructor)
    }

    lateinit var renderHud: FakeKProperty<Boolean>
    var initHudAddition = {
        val hudAddition = HUDAddition().also { it.instance = instance }
        ScreenWindow.addScreenAddition { hudAddition }
        renderHud = FakeKProperty({hudAddition.renderHUD}) {hudAddition.renderHUD = it}
    }

    var init = {
        gui = MainToolgunGUIWindow()
        gui.currentWindow = ToolgunGUI(gui.mainWindow, this)
        externalWindows.forEach { gui.addWindow(it.first, it.second) }
        gui.initGUI()
    }

    var initEvents = {
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
                if (it != Minecraft.getInstance().player) {return@register}
                currentMode = null
                HUDAddition.refreshHUD()
            }

            PersistentEvents.keyPress.on {
                    (keyCode, scanCode, action, modifiers), _ ->
                if (!playerIsUsingToolgun()) {return@on false}
                if (Minecraft.getInstance().screen != null && !toolgunGuiIsOpened()) {return@on false}

                val guiIsOpened = toolgunGuiIsOpened()
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
                    renderHud.set(!renderHud.get())
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
        }}
    }

    var initState = {
        initHudAddition()
        initEvents()
    }

    init {
        instance.client = this
    }

    open fun openGUI() {
        gui.onGUIOpen()
        Minecraft.getInstance().setScreen(gui)
    }

    open fun closeGUI() {
        Minecraft.getInstance().setScreen(null)
    }

    open fun closeWithError(error: String) {
        closeGUI()
        ErrorAddition.addHUDError(error)
    }

    internal open fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): Boolean {
        val cancel = if (currentMode == null) { false } else { currentMode!!.onKeyEvent(keyCode, scanCode, action, modifiers) }
        if (cancel) { return true }

        if (action == GLFW.GLFW_PRESS && TOOLGUN_REMOVE_TOP_VENTITY.matches(keyCode, scanCode)) {
            instance.server.c2sRequestRemoveLastVEntity.sendToServer(EmptyPacket())
            return true
        }

        return false
    }

    internal open fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        if (currentMode == null) {
            return when(button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> EventResult.interruptFalse()
                else -> EventResult.pass()
            }
        }
        return currentMode!!.onMouseButtonEvent(button, action, modifiers)
    }

    internal open fun handleMouseScrollEvent(amount: Double): EventResult {
        if (currentMode == null) { return EventResult.pass() }
        return currentMode!!.onMouseScrollEvent(amount)
    }

    protected open lateinit var gui: MainToolgunGUIWindow

    var toolgunGuiIsOpened: () -> Boolean = {Minecraft.getInstance().screen.let { it is MainToolgunGUIWindow || it is VEntityChangerGui }}
    var playerIsUsingToolgun: () -> Boolean = {Minecraft.getInstance().player?.mainHandItem?.item == VMItems.TOOLGUN.get().asItem()}

    override fun close() {
        currentMode = null
    }
}