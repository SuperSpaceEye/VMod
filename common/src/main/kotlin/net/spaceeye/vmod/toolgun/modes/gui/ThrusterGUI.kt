package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode
import net.spaceeye.vmod.translate.THRUSTER

//TODO move to translatable some day in the future
interface ThrusterGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = THRUSTER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as ThrusterMode

        makeTextEntry("Channel", ::channel, 2f, 2f, parentWindow, ServerLimits.instance.channelLength)
        makeTextEntry("Force", ::force, 2f, 2f, parentWindow, DoubleLimit(1.0))
        makeTextEntry("Scale", ::scale, 2f, 2f, parentWindow, ServerLimits.instance.thrusterScale)
    }
}