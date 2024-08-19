package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import net.spaceeye.vmod.translate.*

interface ScaleGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = SCALE

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as ScaleMode
        makeTextEntry(SCALE.get(), ::scale, 2.0f, 2.0f, parentWindow, DoubleLimit(0.0))
        makeDropDown("Scaling Mode", parentWindow, 2.0f, 2.0f, listOf(
            DItem("Scale All Connected", scaleAllConnected) {scaleAllConnected = true},
            DItem("Scale Single Ship", !scaleAllConnected) {scaleAllConnected = false}
        ))
    }
}