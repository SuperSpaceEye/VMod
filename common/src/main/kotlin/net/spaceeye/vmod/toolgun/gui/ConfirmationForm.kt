package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeText
import java.awt.Color

//TODO Use it where necessary
class ConfirmationForm(question: String, yes: String, no: String, confirmFn: () -> Unit, cancelFn: () -> Unit = {}): UIBlock(Color.BLACK) {
    var filename = ""

    init {
        constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = 200.pixels()
            height = 100.pixels()
        }

        val white = UIBlock(Color.WHITE) constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width  = 200.pixels - 10.pixels
            height = 100.pixels - 10.pixels
        } childOf this

        makeText(question, Color.BLACK, 2f, 2f, white)

        Button(Color.GRAY, yes) {
            parent.removeChild(this)
            confirmFn()
        }.constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 2.pixels()

            width = 100.percent - 4.pixels
        } childOf white

        Button(Color.GRAY, no) {
            parent.removeChild(this)
            cancelFn()
        }.constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 2.pixels()

            width = 100.percent - 4.pixels
        } childOf white
    }
}