package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.utils.FakeKProperty

interface RopeGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = ROPE

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as RopeMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        val sidesT: () -> UIComponent
        val twistingT: () -> UIComponent

        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(STIFFNESS.get(),      ::stiffness,     offset, offset, parentWindow, limits.stiffness)
//        makeTextEntry(DAMPING.get(),        ::damping,       offset, offset, parentWindow, limits.damping)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(WIDTH.get(),          ::width,         offset, offset, parentWindow, ClientLimits.instance.ropeRendererWidth)
        makeCheckBox(FULLBRIGHT.get(),      ::fullbright,    offset, offset, parentWindow)
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, ClientLimits.instance.ropeRendererSegments)

        val fake = FakeKProperty({useTubeRenderer}, {useTubeRenderer = it; if (it) {sidesT().unhide(); twistingT().unhide()} else {sidesT().hide(); twistingT().hide()}})
        makeCheckBox(USE_TUBE_RENDERER.get(), fake, offset, offset, parentWindow)
        makeTextEntry(SIDES.get(), ::sides, offset, offset, parentWindow).also { sidesT = {it} }
        makeCheckBox(ALLOW_TWISTING.get(), ::allowTwisting, offset, offset, parentWindow).also { twistingT = {it} }

        fake.set(useTubeRenderer)
    }
}