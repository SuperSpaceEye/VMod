package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.*
import net.minecraft.network.chat.Component
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.ToggleButton
import net.spaceeye.vmod.translate.get
import java.awt.Color

class MainToolgunGUIWindow(
    val drawTopButtons: Boolean = true
): WindowScreen(ElementaVersion.V8) {
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

    private fun UIBlock.constrain(): UIBlock {
        this constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = 90f.percent()
            height = 90f.percent()
        }

        return this
    }

    internal var currentWindow: ToolgunWindow? = null
    private var windows: MutableList<DItem> = mutableListOf()
    private var buttons = mutableListOf<ToggleButton>()

    fun addWindow(name: Component, constructor: (UIBlock) -> ToolgunWindow) {
        windows.add(DItem(name.get(), false) { currentWindow = constructor(mainWindow.constrain()) })
    }

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

                mainWindow.constrain()
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

    fun initGUI() {
        if (drawTopButtons) {
            buildButtons()
            buttons[0].setDisplay(true)
            buttons[0].updateColor()
        }
    }
}