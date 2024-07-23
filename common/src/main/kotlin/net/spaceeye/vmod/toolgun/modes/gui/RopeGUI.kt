package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.IntLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesGUI
import net.spaceeye.vmod.translate.*

interface RopeGUI: GUIBuilder, PlacementModesGUI {
    override val itemName get() = ROPE

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as RopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(),     ::compliance,    offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(WIDTH.get(),          ::width,         offset, offset, parentWindow, DoubleLimit(0.0, 1.0)) //TODO those
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, IntLimit(1, 100))

        pmMakePlacementModesGUIPart(parentWindow, offset)
    }
}