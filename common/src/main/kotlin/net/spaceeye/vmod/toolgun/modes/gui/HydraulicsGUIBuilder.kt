package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.constraintsManaging.types.HydraulicsMConstraint.ConnectionMode
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

interface HydraulicsGUIBuilder: GUIBuilder {
    override val itemName get() = HYDRAULICS

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as HydraulicsMode
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(WIDTH.get(), ::width, offset, offset, parentWindow, DoubleLimit(0.0, 1.0)) //TODO this
        makeTextEntry(PLACEMENT_ASSIST_SCROLL_STEP.get(), ::paScrollAngleDeg, offset, offset, parentWindow, DoubleLimit())

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)

        makeTextEntry(FIXED_DISTANCE.get(), ::fixedMinLength, offset, offset, parentWindow)

        makeTextEntry(EXTENSION_DISTANCE.get(), ::extensionDistance, offset, offset, parentWindow, DoubleLimit())
        makeTextEntry(EXTENSION_SPEED.get(), ::extensionSpeed, offset, offset, parentWindow, limits.extensionSpeed)

        makeTextEntry(DISTANCE_FROM_BLOCK.get(), ::paDistanceFromBlock, offset, offset, parentWindow, limits.distanceFromBlock)

        makeTextEntry(CHANNEL.get(), ::channel, offset, offset, parentWindow, limits.channelLength)

        makeDropDown(
            HYDRAULICS_INPUT_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(TOGGLE.get(), messageModes == net.spaceeye.vmod.network.MessageModes.Toggle) { messageModes = net.spaceeye.vmod.network.MessageModes.Toggle },
            DItem(SIGNAL.get(), messageModes == net.spaceeye.vmod.network.MessageModes.Signal) { messageModes = net.spaceeye.vmod.network.MessageModes.Signal }
        ))

        makeDropDown(
            HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))

        makeDropDown("Connection Modes", parentWindow, offset, offset, listOf(
            DItem("Fixed Orientation", connectionMode == ConnectionMode.FIXED_ORIENTATION) { connectionMode = ConnectionMode.FIXED_ORIENTATION },
            DItem("Hinge Orientation", connectionMode == ConnectionMode.HINGE_ORIENTATION) { connectionMode = ConnectionMode.HINGE_ORIENTATION },
            DItem("Free Orientation",  connectionMode == ConnectionMode.FREE_ORIENTATION)  { connectionMode = ConnectionMode.FREE_ORIENTATION },
        ))
    }
}