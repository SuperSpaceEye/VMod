package net.spaceeye.vmod.toolgun.modes

import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.toolgun.ToolgunKeybinds
import net.spaceeye.vmod.translate.DEFAULT_HUD_GUI
import net.spaceeye.vmod.translate.get
import java.awt.Color

class DefaultHUD(val name: String = "VMod", val subtextMaker: () -> String = {DEFAULT_HUD_GUI.get().replace("==GUI_MENU_OPEN_OR_CLOSE==", ToolgunKeybinds.GUI_MENU_OPEN_OR_CLOSE.translatedKeyMessage.string)}): HUDBuilder {
    override fun makeHUD(screen: UIContainer) {
        val background = GradientComponent(
            Color(40, 40, 40),
            Color(40, 40, 40, 0),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT) constrain {
            x = 0.percent
            y = 2.percent

            width = ChildBasedMaxSizeConstraint() + 10.pixels
            height = ChildBasedSizeConstraint() + 10.pixels
        } childOf screen

        val textHolder = UIContainer() constrain  {
            x = 0.percent
            y = 0.percent

            width = ChildBasedMaxSizeConstraint() + 20.pixels
            height = ChildBasedSizeConstraint()
        } childOf background

        UIText(name, false) constrain {
            x = 5.pixels
            y = 5.pixels

            textScale = 2.pixels
        } childOf textHolder

        val makeText = {text: String ->
            UIWrappedText(text, false) constrain {
                x = 5.pixels
                y = SiblingConstraint() + 2.pixels

                textScale = 1.pixels
            } childOf textHolder
            Unit
        }

        makeText(subtextMaker())
    }
}