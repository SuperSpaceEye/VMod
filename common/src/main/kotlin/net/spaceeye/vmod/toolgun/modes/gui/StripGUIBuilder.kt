package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.Component
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.GUIComponents

interface StripGUIBuilder: GUIBuilder {
    override val itemName: Component
        get() = GUIComponents.STRIP

    override fun makeGUISettings(parentWindow: UIBlock) {}
}