package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.COPY

interface CopyGUIBuilder: GUIBuilder {
    override val itemName get() = COPY
    override fun makeGUISettings(parentWindow: UIBlock) {}
}