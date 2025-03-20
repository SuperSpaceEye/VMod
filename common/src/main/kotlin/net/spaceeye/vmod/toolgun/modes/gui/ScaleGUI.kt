package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import net.spaceeye.vmod.translate.*

interface ScaleGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = SCALE

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as ScaleMode
        makeTextEntry(SCALE.get(), ::scale, 2.0f, 2.0f, parentWindow, ServerLimits.instance.scale)
        makeDropDown(SCALING_MODE.get(), parentWindow, 2.0f, 2.0f, listOf(
            DItem(SCALE_ALL_CONNECTED.get(), scaleAllConnected) {scaleAllConnected = true},
            DItem(SCALE_SINGLE_SHIP.get(), !scaleAllConnected) {scaleAllConnected = false}
        ))
    }
}