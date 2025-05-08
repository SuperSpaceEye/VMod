package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.*

interface VEntityChangerGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = VENTITY_CHANGER

    override fun eMakeGUISettings(parentWindow: UIContainer) {}
}