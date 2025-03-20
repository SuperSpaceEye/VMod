package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode
import net.spaceeye.vmod.translate.CHANNEL
import net.spaceeye.vmod.translate.FORCE
import net.spaceeye.vmod.translate.FULLBRIGHT
import net.spaceeye.vmod.translate.SSCALE
import net.spaceeye.vmod.translate.THRUSTER
import net.spaceeye.vmod.translate.get

interface ThrusterGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = THRUSTER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as ThrusterMode

        makeTextEntry(CHANNEL.get(), ::channel, 2f, 2f, parentWindow, ServerLimits.instance.channelLength)
        makeTextEntry(FORCE.get(), ::force, 2f, 2f, parentWindow, ServerLimits.instance.thrusterForce)
        makeTextEntry(SSCALE.get(), ::scale, 2f, 2f, parentWindow, ServerLimits.instance.thrusterScale)
        makeCheckBox(FULLBRIGHT.get(), ::fullbright, 2f, 2f, parentWindow)
    }
}