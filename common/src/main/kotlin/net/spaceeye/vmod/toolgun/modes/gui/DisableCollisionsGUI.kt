package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.*

interface DisableCollisionsGUI: GUIBuilder {
    override val itemName get() = DISABLE_COLLISIONS

    override fun makeGUISettings(parentWindow: UIContainer) {}
}