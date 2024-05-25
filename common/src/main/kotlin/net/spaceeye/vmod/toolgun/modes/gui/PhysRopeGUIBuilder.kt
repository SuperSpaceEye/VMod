package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.PhysRopeMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

interface PhysRopeGUIBuilder: GUIBuilder {
    override val itemName get() = PHYS_ROPE

    override fun makeGUISettings(parentWindow: UIBlock) {
        this as PhysRopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(),     ::compliance,    offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, limits.physRopeSegments)
        makeTextEntry(MASS_PER_SEGMENT.get(),::massPerSegment,offset,offset, parentWindow, limits.physRopeMassPerSegment)
        makeTextEntry(RADIUS.get(),         ::radius,        offset, offset, parentWindow, limits.physRopeRadius)
        makeDropDown(
            HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
        ))
    }
}