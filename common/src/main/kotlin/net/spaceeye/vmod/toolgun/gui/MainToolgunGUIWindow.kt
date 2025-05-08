package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.ToggleButton
import net.spaceeye.vmod.translate.CLIENT_SETTINGS
import net.spaceeye.vmod.translate.MAIN
import net.spaceeye.vmod.translate.SERVER_SETTINGS
import net.spaceeye.vmod.translate.get
import java.awt.Color

class MainToolgunGUIWindow(): WindowScreen(ElementaVersion.V8) {
    class ButtonsXConstraint(
        val mainWindow: UIBlock,
    ): XConstraint {
        override var cachedValue: Float = 0f
        override var constrainTo: UIComponent? = null
        override var recalculate: Boolean = true

        override fun getXPositionImpl(component: UIComponent): Float {
            return mainWindow.getLeft()
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
            return super.visit(visitor, type, true)
        }
    }
    class ButtonsYConstraint(
        val mainWindow: UIBlock,
    ): YConstraint {
        override var cachedValue: Float = 0f
        override var constrainTo: UIComponent? = null
        override var recalculate: Boolean = true

        override fun getYPositionImpl(component: UIComponent): Float {
            return mainWindow.getTop() - component.getHeight()
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
            return super.visit(visitor, type, true)
        }
    }

    internal val mainWindow = UIBlock(Color(200, 200, 200)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90f.percent()
        height = 90f.percent()
    } childOf window

    internal var currentWindow: ToolgunWindow? = null

    internal var windows = mutableListOf(
        DItem(MAIN.get(), true) {currentWindow = ToolgunGUI(mainWindow)},
        DItem(CLIENT_SETTINGS.get(), false) {currentWindow = ClientSettingsGUI(mainWindow)},
        DItem(SERVER_SETTINGS.get(), false) {currentWindow = ServerSettingsGUI(mainWindow)},
//        DItem("Settings Presets", false) {}
    )

    private var buttons = mutableListOf<ToggleButton>()

    fun onGUIOpen() {
        currentWindow?.onGUIOpen()
    }

    private fun buildButtons() {
        val btns = UIContainer()
        for ((i, state) in windows.withIndex()) {
            var btn: ToggleButton? = null
            btn = ToggleButton(Color(200, 200, 200), Color.GREEN, state.name, state.highlight) {
                mainWindow.clearChildren()

                buttons.forEach { it.state = false; it.setDisplay(false); it.updateColor() }
                btn!!.setDisplay(true)
                btn!!.state = true

                state.fnToApply()
            } constrain {
                width = ChildBasedSizeConstraint() + 2.pixels
                height = ChildBasedSizeConstraint() + 2.pixels

                x = SiblingConstraint() + if (i != 0) 2.pixels else 0.pixels
                y = CenterConstraint()
            } childOf btns

            buttons.add(btn)
        }

        btns constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()

            x = ButtonsXConstraint(mainWindow)
            y = ButtonsYConstraint(mainWindow)
        } childOf window

    }

    init {
        buildButtons()
        currentWindow = ToolgunGUI(mainWindow)
        buttons[0].setDisplay(true)
        buttons[0].updateColor()
    }
}