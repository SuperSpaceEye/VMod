package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import net.spaceeye.vmod.translate.*

interface ScaleGUI: GUIBuilder {
    override val itemName get() = SCALE

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as ScaleMode
        makeTextEntry(SCALE.get(), ::scale, 2.0f, 2.0f, parentWindow, DoubleLimit(0.0))
    }
}