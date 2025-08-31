package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeFolder
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.PhysRopeMode
import net.spaceeye.vmod.translate.*

interface PhysRopeGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = PHYS_ROPE

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as PhysRopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(STIFFNESS.get(),      ::stiffness,     offset, offset, parentWindow, limits.stiffness)
        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, limits.physRopeSegments)
        makeTextEntry(TOTAL_MASS.get(),     ::totalMass,     offset, offset, parentWindow, limits.totalMassOfPhysRope)
        makeTextEntry(RADIUS.get(),         ::radius,        offset, offset, parentWindow, limits.physRopeRadius)
        makeTextEntry(ANGLE_LIMIT.get(),    ::angleLimit,    offset, offset, parentWindow, limits.physRopeAngleLimit)
        makeTextEntry(SIDES.get(),          ::sides,         offset, offset, parentWindow, limits.physRopeSides)
        makeCheckBox(FULLBRIGHT.get(),      ::fullbright,    offset, offset, parentWindow)

        makeFolder(TEXTURE_OPTIONS.get(), parentWindow, offset, offset) { parentWindow ->
            makeTextEntry(LENGTH_UV_START.get(),           ::lengthUVStart, offset, offset, parentWindow)
            makeTextEntry(LENGTH_UV_STEP_MULTIPLIER.get(), ::lengthUVIncMultiplier, offset, offset, parentWindow)
            makeTextEntry(WIDTH_UV_START.get(),            ::widthUVStart, offset, offset, parentWindow)
            makeTextEntry(WIDTH_UV_MULTIPLIER.get(),       ::widthUVMultiplier, offset, offset, parentWindow)
        }
    }
}