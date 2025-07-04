package net.spaceeye.vmod.toolgun

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import gg.essential.elementa.components.UIBlock
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.PlatformUtils
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.gui.SettingPresets
import net.spaceeye.vmod.toolgun.gui.ToolgunWindow
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.state.VEntityChangerGui
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.ClientClosable
import net.spaceeye.vmod.utils.EmptyPacket
import org.lwjgl.glfw.GLFW

fun CELOG(s: String, toShow: String) {
    ELOG(s)
    ClientToolGunState.addHUDError(toShow)
}
fun CELOG(s: String, toShow: Component) = CELOG(s, toShow.get())

fun CERROR(toShow: String) = ClientToolGunState.addHUDError(toShow)
fun CERROR(toShow: Component) = CERROR(toShow.get())

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

    init {
        ScreenWindow
        SettingPresets
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
            if (it != Minecraft.getInstance().player) {return@register}
            currentMode = null
            refreshHUD()
        }
        ClientLifecycleEvent.CLIENT_STARTED.register {
            initScreen()
        }
    }

    fun refreshHUD() { screen?.getExtensionOfType<HUDAddition>()?.refreshHUD() }

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

    val TOOLGUN_TOGGLE_HUD_KEY = register(
        KeyMapping(
            "key.vmod.toggle_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_TOGGLE_HUD_INFO_KEY = register(
        KeyMapping(
            "key.vmod.toggle_hud_info",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_I,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_CHANGE_PRESET_KEY = register(
        KeyMapping(
            "key.vmod.change_preset_key",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
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
        screen?.getExtensionOfType<ErrorAddition>()?.addError(str)
    }

    var renderHud = true

    fun initScreen() {
        screen = ScreenWindow.makeScreen()
        // why? it doesn't correctly remap in dev env when added as a dependency for another project for some reason, this works though
        PlatformUtils.initScreen(screen!!)
    }

    internal fun onRenderHUD(stack: GuiGraphics, delta: Float) {
        if (!renderHud) {return}
        try {
        (screen ?: run {
            initScreen()
            screen!!
        }).onRenderHUD(stack, delta)
        } catch (e: Exception) { ELOG("HUD rendering failed\n${e.stackTraceToString()}")
        } catch (e: Error) { ELOG("HUD rendering failed\n${e.stackTraceToString()}") }
    }

    private lateinit var gui: MainToolgunGUIWindow

    //TODO redo this
    internal fun guiIsOpened() = Minecraft.getInstance().screen.let { it is MainToolgunGUIWindow || it is VEntityChangerGui }
    internal fun otherGuiIsOpened() = Minecraft.getInstance().screen.let { it != null && it !is MainToolgunGUIWindow && it !is VEntityChangerGui }

    fun playerIsUsingToolgun(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        return player.mainHandItem.item == VMItems.TOOLGUN.get().asItem()
    }

    private val externalWindows = mutableListOf<Pair<Component, (UIBlock) -> ToolgunWindow>>()

    internal fun init() {
        gui = MainToolgunGUIWindow()
        externalWindows.forEach { gui.windows.add(DItem(it.first.get(), false) { gui.currentWindow =  it.second.invoke(gui.mainWindow)}) }
    }

    fun addWindow(name: Component, windowConstructor: (UIBlock) -> ToolgunWindow) {
        externalWindows.add(name to windowConstructor)
    }

    override fun close() {
        currentMode = null
    }
}