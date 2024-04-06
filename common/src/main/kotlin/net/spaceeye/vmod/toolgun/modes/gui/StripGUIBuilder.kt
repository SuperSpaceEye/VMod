package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.Component
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.StripMode
import net.spaceeye.vmod.translate.GUIComponents
import net.spaceeye.vmod.translate.GUIComponents.RADIUS
import net.spaceeye.vmod.translate.GUIComponents.STRIP_ALL
import net.spaceeye.vmod.translate.GUIComponents.STRIP_IN_RADIUS
import net.spaceeye.vmod.translate.GUIComponents.STRIP_MODES
import net.spaceeye.vmod.translate.get

interface StripGUIBuilder: GUIBuilder {
    override val itemName: Component
        get() = GUIComponents.STRIP

    override fun makeGUISettings(parentWindow: UIBlock) {
        this as StripMode

        makeTextEntry(RADIUS.get(), ::radius, 2f, 2f, parentWindow, ServerLimits.instance.radius)
        makeDropDown(STRIP_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(STRIP_ALL.get(), mode == StripMode.StripModes.StripAll) {mode = StripMode.StripModes.StripAll},
            DItem(STRIP_IN_RADIUS.get(), mode == StripMode.StripModes.StripInRadius) {mode = StripMode.StripModes.StripInRadius}
        ))
    }
}