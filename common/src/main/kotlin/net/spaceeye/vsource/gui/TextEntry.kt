package net.spaceeye.vsource.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color

class TextEntry(strName: String, text_scale: Float = 1f, fnToApply: (str: String) -> Unit): UIBlock() {
    val textArea: UITextInput

    init {
        constrain {
            width = FillConstraint()
            height = ChildBasedMaxSizeConstraint() + 4.pixels
        }

        val strHolder = UIBlock().constrain {
            x = 4.pixels()
            y = CenterConstraint()

            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedMaxSizeConstraint()
        } childOf this

        val str = UIText(strName, shadow = false).constrain {
            y = CenterConstraint()

            textScale = text_scale.pixels()

            color = Color(0, 0, 0).toConstraint()
        } childOf strHolder

        val textHolder = UIBlock(Color(170, 170, 170)).constrain {
            x = SiblingConstraint()
            y = CenterConstraint()

            width = FillConstraint() - 4.pixels() - 4.pixels()
            height = (textScale * 9f)
        } childOf this
        textArea = (((UITextInput().constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = FillConstraint()
            height = FillConstraint()

            color = Color.BLACK.toConstraint()
        }.onMouseClick {
            grabWindowFocus()
        }) as UITextInput).onUpdate(fnToApply) childOf textHolder) as UITextInput
    }
}