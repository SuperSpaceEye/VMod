package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.COMChangerMode
import net.spaceeye.vmod.translate.*

interface COMChangerGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = COM_CHANGER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as COMChangerMode
    }
}