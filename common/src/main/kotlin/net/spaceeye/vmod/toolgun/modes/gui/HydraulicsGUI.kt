package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import net.spaceeye.vmod.vEntityManaging.types.constraints.HydraulicsConstraint.ConnectionMode
import net.spaceeye.vmod.guiElements.ColorPicker
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import net.spaceeye.vmod.translate.*

interface HydraulicsGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = HYDRAULICS

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as HydraulicsMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(WIDTH.get(), ::width, offset, offset, parentWindow, ClientLimits.instance.lineRendererWidth)
        makeCheckBox(FULLBRIGHT.get(), ::fullbright, offset, offset, parentWindow)

        makeTextEntry(MAX_FORCE.get(), ::maxForce, offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(STIFFNESS.get(), ::stiffness, offset, offset, parentWindow, limits.stiffness)
        makeTextEntry(DAMPING.get(), ::damping, offset, offset, parentWindow, limits.damping)

        makeTextEntry(FIXED_DISTANCE.get(), ::fixedMinLength, offset, offset, parentWindow)

        makeTextEntry(EXTENSION_DISTANCE.get(), ::extensionDistance, offset, offset, parentWindow)
        makeTextEntry(EXTENSION_SPEED.get(), ::extensionSpeed, offset, offset, parentWindow, limits.extensionSpeed)

        makeTextEntry(CHANNEL.get(), ::channel, offset, offset, parentWindow, limits.channelLength)

        makeDropDown(CONNECTION_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(FIXED.get(),   connectionMode == ConnectionMode.FIXED_ORIENTATION) { connectionMode = ConnectionMode.FIXED_ORIENTATION },
            DItem(BEARING.get(), connectionMode == ConnectionMode.HINGE_ORIENTATION) { connectionMode = ConnectionMode.HINGE_ORIENTATION },
            DItem(FREE.get(),    connectionMode == ConnectionMode.FREE_ORIENTATION)  { connectionMode = ConnectionMode.FREE_ORIENTATION },
        ))

        ColorPicker(color) {
            color = it
        } constrain {
            x = offset.pixels()
            y = SiblingConstraint() + offset.pixels()
        } childOf parentWindow
    }
}