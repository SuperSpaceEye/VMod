package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode

interface ThrusterGUI: GUIBuilder {
    override val itemName get() = TranslatableComponent("Thruster")

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as ThrusterMode

        makeTextEntry("Channel", ::channel, 2f, 2f, parentWindow, StrLimit(50))
        makeTextEntry("Force", ::force, 2f, 2f, parentWindow, DoubleLimit(1.0))
    }
}