package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.*

interface DisableCollisionsGUIBuilder: GUIBuilder {
    override val itemName get() = DISABLE_COLLISIONS

    override fun makeGUISettings(parentWindow: UIBlock) {}
}