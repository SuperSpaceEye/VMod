package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.ToggleButton
import java.awt.Color

class MainToolgunGUIWindow(): WindowScreen(ElementaVersion.V5) {
    private val mainWindow = UIBlock(Color(200, 200, 200)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90f.percent()
        height = 90f.percent()
    } childOf window

    private var currentWindow: ToolgunWindow? = null

    private var windows = mutableListOf(
        DItem("Main", true) {currentWindow = ToolgunGUI(mainWindow)},
//        DItem("Client Settings", false) {},
        DItem("Server Settings", false) {currentWindow = ServerSettingsGUI(mainWindow)},
//        DItem("Settings Presets", false) {}
    )

    private var buttons = mutableListOf<ToggleButton>()

    fun onGUIOpen() {
        currentWindow?.onGUIOpen()
    }

    private fun getXPos(): Double {
        return buttons.sumOf {it: ToggleButton -> (it.getWidth() + 2f).toDouble() }
    }

    private fun buildButtons() {
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

                if (i == 0) {
                    x = (mainWindow.getLeft() + getXPos()).pixels
                    y = (mainWindow.getTop() - getHeight()).pixels
                } else {
                    x = (mainWindow.getLeft() + getXPos()).pixels
                    y = (mainWindow.getTop() - getHeight()).pixels
                }

            } childOf window

            buttons.add(btn)
        }
    }

    init {
        buildButtons()
        currentWindow = ToolgunGUI(mainWindow)
        buttons[0].setDisplay(true)
        buttons[0].updateColor()
    }
}