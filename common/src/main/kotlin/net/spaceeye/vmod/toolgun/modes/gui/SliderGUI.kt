package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.vEntityManaging.types.constraints.SliderConstraint.ConnectionMode
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import net.spaceeye.vmod.translate.*

interface SliderGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = SLIDER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as SliderMode

        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(MAX_FORCE.get(), ::maxForce, offset, offset, parentWindow, limits.maxForce)

        makeDropDown(
            CONNECTION_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(FIXED.get(),   connectionMode == ConnectionMode.FIXED_ORIENTATION) { connectionMode = ConnectionMode.FIXED_ORIENTATION },
            DItem(BEARING.get(), connectionMode == ConnectionMode.HINGE_ORIENTATION) { connectionMode = ConnectionMode.HINGE_ORIENTATION },
            DItem(FREE.get(),    connectionMode == ConnectionMode.FREE_ORIENTATION)  { connectionMode = ConnectionMode.FREE_ORIENTATION },
        ))
    }
}