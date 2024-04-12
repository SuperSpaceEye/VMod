package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.AxisMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

interface AxisGUIBuilder: GUIBuilder {
    override val itemName get() = AXIS

    override fun makeGUISettings(parentWindow: UIBlock) {
        this as AxisMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(WIDTH.get(), ::width, offset, offset, parentWindow, DoubleLimit(0.0, 1.0)) //TODO this
        makeTextEntry(PLACEMENT_ASSIST_SCROLL_STEP.get(), ::paScrollAngleDeg, offset, offset, parentWindow, DoubleLimit())

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)

        makeTextEntry(FIXED_DISTANCE.get(),     ::fixedDistance,     offset, offset, parentWindow)
        makeCheckBox (DISABLE_COLLISIONS.get(), ::disableCollisions, offset, offset, parentWindow)
        makeTextEntry(DISTANCE_FROM_BLOCK.get(),::paDistanceFromBlock,offset,offset, parentWindow, limits.distanceFromBlock)
        makeDropDown(
            HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }
}