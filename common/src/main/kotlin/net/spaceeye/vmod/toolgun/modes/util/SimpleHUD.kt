package net.spaceeye.vmod.toolgun.modes.util

import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.EHUDBuilder
import net.spaceeye.vmod.toolgun.modes.HUDBuilder
import net.spaceeye.vmod.translate.get
import java.awt.Color

//TODO remove later
interface SimpleHUD: HUDBuilder {
    fun makeSubText(makeText: (String) -> Unit)
    fun makeSubText(makeText: (String) -> Unit, textHolder: UIContainer) { makeSubText(makeText) }

    override fun makeHUD(screen: UIContainer) {
        this as BaseMode

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

        UIText(itemName.get(), false) constrain {
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

        makeSubText(makeText, textHolder)
    }
}

interface ESimpleHUD: EHUDBuilder {
    fun makeSubText(makeText: (String) -> Unit)
    fun makeSubText(makeText: (String) -> Unit, textHolder: UIContainer) { makeSubText(makeText) }

    override fun eMakeHUD(screen: UIContainer) {
        this as BaseMode

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

        UIText(itemName.get(), false) constrain {
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

        makeSubText(makeText, textHolder)
    }
}