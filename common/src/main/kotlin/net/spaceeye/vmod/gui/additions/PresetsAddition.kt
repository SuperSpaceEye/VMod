package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientRawInputEvent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ClientToolGunState.playerIsUsingToolgun
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.SettingPresets.Companion.listPresets
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

object PresetsAddition: ScreenWindowAddition {
    private lateinit var screenContainer: UIContainer

    private var presetsContainer: UIBlock = UIBlock(Color(80, 80, 80, 127)) constrain {
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    }

    var render = false
    var presetPaths = emptyList<Path>()
    var chosenPreset = Path.of("No Preset")

    var lastPresets = emptySet<Path>()

    init {
        PersistentEvents.keyPress.on { (keyCode, scanCode, action, modifiers), _ ->
            if (!ClientToolGunState.TOOLGUN_CHANGE_PRESET_KEY.matches(keyCode, scanCode)) { return@on false }

            if (!playerIsUsingToolgun()) return@on false
            val mode = ClientToolGunState.currentMode as? ExtendableToolgunMode ?: return@on false
            if (mode.getExtensionsOfType<Presettable>().isEmpty()) return@on false

            if (action == GLFW.GLFW_PRESS) {
                val presets = listPresets(mode::class.simpleName!!)
                if (presets.isEmpty()) return@on false

                presetPaths = presets
                render = true
            } else if (action == GLFW.GLFW_RELEASE) {
                render = false
            }

            return@on true
        }

        ClientRawInputEvent.MOUSE_SCROLLED.register {
                client, amount ->
            if (!render) {return@register EventResult.pass() }
            if (!playerIsUsingToolgun()) {return@register EventResult.pass()}

            val presetPaths = presetPaths.toMutableList()
            presetPaths.add(0, Path.of("No Preset"))

            //TODO O(n) but do i care?
            val curPos = presetPaths.indexOf(chosenPreset)
            val newPos = max(min(curPos + amount.sign.toInt(), 0), presetPaths.size-1)
            chosenPreset = presetPaths[newPos]

            presetsContainer.childrenOfType<UIWrappedText>().forEach {
                it constrain {
                    color = (if (it.getText() == chosenPreset.nameWithoutExtension) Color.GREEN else Color.WHITE).toConstraint()
                }
            }

            return@register EventResult.interruptFalse()
        }
    }

    override fun init(screenContainer: UIContainer) {
        this.screenContainer = screenContainer
        presetsContainer childOf screenContainer
        presetsContainer.hide()
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        if (!render || !playerIsUsingToolgun()) {
            presetsContainer.hide()
            return
        }
        if (lastPresets.equals(presetPaths)) return
        lastPresets = presetPaths.toSet()
        chosenPreset = Path.of("No Preset")

        val presets = presetPaths.toMutableList()
        presets.add(0, chosenPreset)

        presetsContainer.clearChildren()

        presets.forEach { path ->
            makeText(path.nameWithoutExtension, if (path == chosenPreset) Color.GREEN else Color.WHITE, 2f, 2f, presetsContainer)
        }

        presetsContainer.unhide()

        presetsContainer.constrain {
            x = 100.percent - presetsContainer.getWidth().pixels - 4.pixels
            y = 100.percent - presetsContainer.getWidth().pixels

            width = ChildBasedMaxSizeConstraint() + 4.pixels
            height = ChildBasedSizeConstraint() + (presetPaths.size * 2).pixels + 2.pixels

            color = Color(80, 80, 80, 220).toConstraint()
        }
    }
}