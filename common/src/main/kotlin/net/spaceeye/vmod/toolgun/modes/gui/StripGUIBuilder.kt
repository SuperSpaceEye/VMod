package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIBlock
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.GUIComponents

interface StripGUIBuilder: GUIBuilder {
    override val itemName: TranslatableComponent
        get() = GUIComponents.STRIP

    override fun makeGUISettings(parentWindow: UIBlock) {}
}