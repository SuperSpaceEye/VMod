package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientRawInputEvent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.guiElements.ScrollMenu
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.SettingPresets.Companion.fromJsonStr
import net.spaceeye.vmod.toolgun.gui.SettingPresets.Companion.listPresets
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.translate.SOMETHING_WENT_WRONG
import net.spaceeye.vmod.translate.get
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class PresetsAddition(): ScreenWindowAddition() {
    private lateinit var screenContainer: UIContainer

    private var presetsContainer = ScrollMenu(10, Color(50, 50, 50, 220), Color.WHITE)

    var render = false
    var presetPaths = emptyList<Path>()
    var chosenPreset = Path.of("No Preset")

    var lastPresets = emptySet<Path>()

    init {
        PersistentEvents.keyPress.on { (keyCode, scanCode, action, modifiers), _ ->
            if (!instance.client.TOOLGUN_CHANGE_PRESET_KEY.matches(keyCode, scanCode)) { return@on false }

            if (!instance.client.playerIsUsingToolgun()) return@on false
            val mode = instance.client.currentMode as? ExtendableToolgunMode ?: return@on false
            if (mode.getExtensionsOfType<Presettable>().isEmpty()) return@on false

            if (action == GLFW.GLFW_PRESS) {
                val presets = listPresets(mode::class.simpleName!!, this.instance.instanceStorage["Presettable-dir-name"] as String).toMutableList()
                if (presets.isEmpty()) return@on false

                presets.add(0, Path.of("No Preset"))

                presetPaths = presets
                render = true
            } else if (action == GLFW.GLFW_RELEASE) {
                render = false

                val jsonStr = try { Files.readString(chosenPreset) } catch (e: Exception) {return@on true}
                val items = mode.getAllReflectableItems(true) {it.metadata.contains("presettable")}
                try {
                    fromJsonStr(jsonStr, items)
                } catch (e: Exception) {
                    ELOG(e.stackTraceToString())
                    ErrorAddition.addHUDError(SOMETHING_WENT_WRONG.get())
                }
            }

            return@on true
        }

        ClientRawInputEvent.MOUSE_SCROLLED.register {
                client, amount ->
            if (!render) {return@register EventResult.pass() }
            if (!instance.client.playerIsUsingToolgun()) {return@register EventResult.pass()}

            //TODO O(n) but do i care?
            val curPos = presetPaths.indexOf(chosenPreset)
            val newPos = min(max(curPos - amount.sign.toInt(), 0), presetPaths.size-1)
            chosenPreset = presetPaths[newPos]

            presetsContainer.updateHighlightedOption(newPos)

            return@register EventResult.interruptFalse()
        }
    }

    override fun init(screenContainer: UIContainer) {
        this.screenContainer = screenContainer
        presetsContainer childOf screenContainer
        presetsContainer.hide()
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        if (!render || !instance.client.playerIsUsingToolgun()) {
            presetsContainer.hide()
            lastPresets = emptySet()
            return
        }
        if (lastPresets.containsAll(presetPaths) && lastPresets.size == presetPaths.size) {
            return
        }
        lastPresets = presetPaths.toSet()
        chosenPreset = Path.of("No Preset")

        presetsContainer.setItems(presetPaths.map { it.nameWithoutExtension })
        presetsContainer.updateHighlightedOption(0)
        presetsContainer.unhide()
        presetsContainer.constrain {
            x = 100.percent - presetsContainer.getWidth().pixels - 4.pixels
            y = 100.percent - presetsContainer.getWidth().pixels
        }
    }
}