package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.MassChangerMode
import net.spaceeye.vmod.translate.*

interface MassChangerGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = MASS_CHANGER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as MassChangerMode

        makeTextEntry(NEW_MASS.get(), ::newMass, 2f, 2f, parentWindow, ServerLimits.instance.massLimit)
    }
}