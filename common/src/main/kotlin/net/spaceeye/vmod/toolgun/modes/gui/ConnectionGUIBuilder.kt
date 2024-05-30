package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.constraintsManaging.types.ConnectionMConstraint.ConnectionModes
import net.spaceeye.vmod.guiElements.ColorPicker
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ConnectionMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

interface ConnectionGUIBuilder: GUIBuilder {
    override val itemName get() = TranslatableComponent("Connection")

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as ConnectionMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(WIDTH.get(), ::width, offset, offset, parentWindow, DoubleLimit(0.0, 1.0)) //TODO this
        makeTextEntry(PLACEMENT_ASSIST_SCROLL_STEP.get(), ::paScrollAngleDeg, offset, offset, parentWindow, DoubleLimit())

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)

        makeTextEntry(FIXED_DISTANCE.get(),     ::fixedDistance,     offset, offset, parentWindow)

        makeTextEntry(DISTANCE_FROM_BLOCK.get(), ::paDistanceFromBlock, offset, offset, parentWindow, limits.distanceFromBlock)
        makeDropDown(
            HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))

        makeDropDown("Connection Modes", parentWindow, offset, offset, listOf(
            DItem("Fixed Orientation", connectionMode == ConnectionModes.FIXED_ORIENTATION) { connectionMode = ConnectionModes.FIXED_ORIENTATION },
            DItem("Hinge Orientation", connectionMode == ConnectionModes.HINGE_ORIENTATION) { connectionMode = ConnectionModes.HINGE_ORIENTATION },
            DItem("Free Orientation",  connectionMode == ConnectionModes.FREE_ORIENTATION)  { connectionMode = ConnectionModes.FREE_ORIENTATION },
        ))

        ColorPicker(color) {
            color = it
        } constrain {
            x = offset.pixels()
            y = SiblingConstraint() + offset.pixels()
        } childOf parentWindow
    }
}