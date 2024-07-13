package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.PhysRopeMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesGUI
import net.spaceeye.vmod.translate.*

interface PhysRopeGUI: GUIBuilder, PlacementModesGUI {
    override val itemName get() = PHYS_ROPE

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as PhysRopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(),     ::compliance,    offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, limits.physRopeSegments)
        makeTextEntry(MASS_PER_SEGMENT.get(),::massPerSegment,offset,offset, parentWindow, limits.physRopeMassPerSegment)
        makeTextEntry(RADIUS.get(),         ::radius,        offset, offset, parentWindow, limits.physRopeRadius)

        pmMakePlacementModesNoCenteredInBlockGUIPart(parentWindow, offset)
    }
}