package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesGUI
import net.spaceeye.vmod.translate.COMPLIANCE
import net.spaceeye.vmod.translate.MAX_FORCE
import net.spaceeye.vmod.translate.get

interface SliderGUI: GUIBuilder, PlacementModesGUI {
    override val itemName get() = TranslatableComponent("Slider")

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as SliderMode

        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)

        pmMakePlacementModesGUIPart(parentWindow, offset)
    }
}