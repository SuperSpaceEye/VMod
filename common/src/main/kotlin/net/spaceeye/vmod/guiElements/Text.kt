package net.spaceeye.vmod.guiElements

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.toConstraint
import java.awt.Color

fun makeText(text: String, color: Color, xPadding: Float, yPadding: Float, makeChildOf: UIComponent): UIWrappedText {
    return UIWrappedText(text, false).constrain {
        this.color = color.toConstraint()
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()
        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf
}