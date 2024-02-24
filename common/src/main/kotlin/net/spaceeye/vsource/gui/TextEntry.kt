package net.spaceeye.vsource.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color
import kotlin.reflect.KMutableProperty0

class TextEntry(strName: String, text_scale: Float = 1f, fnToApply: (str: String, entry: TextEntry) -> Unit): UIBlock() {
    val textArea: UITextInput
    val textHolder: UIBlock

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

        textHolder = UIBlock(Color(170, 170, 170)).constrain {
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
        }) as UITextInput).onUpdate { fnToApply(it, this) } childOf textHolder) as UITextInput
    }
}

fun makeTextEntry(name: String,
                  value: KMutableProperty0<Double>,
                  xPadding: Float,
                  yPadding: Float,
                  makeChildOf: UIBlock,
                  minValue: Double = -Double.MAX_VALUE,
                  maxValue: Double = Double.MAX_VALUE): TextEntry {
    val entry = TextEntry(name) {
        str, entry ->

        val parsedValue = str.toDoubleOrNull()

        if (parsedValue == null || (parsedValue < minValue || parsedValue > maxValue)) {
            entry.textHolder.setColor(Color(230, 0, 0))
            return@TextEntry
        }
        entry.textHolder.setColor(Color(170, 170, 170))

        value.set(parsedValue)
    }.constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()
        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf
    entry.textArea.setText(value.get().toString())
    return entry
}