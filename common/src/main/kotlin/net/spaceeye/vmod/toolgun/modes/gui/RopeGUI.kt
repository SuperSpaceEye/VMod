package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.translate.*

interface RopeGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = ROPE

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as RopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(STIFFNESS.get(),      ::stiffness,     offset, offset, parentWindow, limits.stiffness)
//        makeTextEntry(DAMPING.get(),        ::damping,       offset, offset, parentWindow, limits.damping)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(WIDTH.get(),          ::width,         offset, offset, parentWindow, ClientLimits.instance.ropeRendererWidth)
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, ClientLimits.instance.ropeRendererSegments)
    }
}