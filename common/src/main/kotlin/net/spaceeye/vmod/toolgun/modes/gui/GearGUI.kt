package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.GearMode
import net.spaceeye.vmod.translate.*

interface GearGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = GEAR

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as GearMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(MAX_FORCE.get(),  ::maxForce,  offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(GEAR_RATIO.get(), ::gearRatio, offset, offset, parentWindow, limits.gearRatio)
    }
}