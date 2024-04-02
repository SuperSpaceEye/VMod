package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import net.spaceeye.vmod.translate.GUIComponents.SCALE
import net.spaceeye.vmod.translate.get

interface ScaleGUIBuilder: GUIBuilder {
    override val itemName: TranslatableComponent
        get() = SCALE

    override fun makeGUISettings(parentWindow: UIBlock) {
        this as ScaleMode
        makeTextEntry(SCALE.get(), ::scale, 2.0f, 2.0f, parentWindow, DoubleLimit(0.0))
    }
}