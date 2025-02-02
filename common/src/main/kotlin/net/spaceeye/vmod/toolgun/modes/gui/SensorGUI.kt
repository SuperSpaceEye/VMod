package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.SensorMode
import net.spaceeye.vmod.translate.CHANNEL
import net.spaceeye.vmod.translate.IGNORE_SELF_SHIP
import net.spaceeye.vmod.translate.MAX_DISTANCE
import net.spaceeye.vmod.translate.SENSOR
import net.spaceeye.vmod.translate.SSCALE
import net.spaceeye.vmod.translate.get

interface SensorGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = SENSOR

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as SensorMode

        makeTextEntry(CHANNEL.get(), ::channel, 2f, 2f, parentWindow, ServerLimits.instance.channelLength)
        makeTextEntry(MAX_DISTANCE.get(), ::maxDistance, 2f, 2f, parentWindow, ServerLimits.instance.maxDistance)
        makeTextEntry(SSCALE.get(), ::scale, 2f, 2f, parentWindow, ServerLimits.instance.sensorScale)
        makeCheckBox(IGNORE_SELF_SHIP.get(), ::ignoreSelf, 2f, 2f, parentWindow)
    }
}