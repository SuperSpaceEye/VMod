package net.spaceeye.vsource.guiElements

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import java.awt.Color
import kotlin.reflect.KMutableProperty0

class CheckBox(
    baseColor: Color,
    activatedColor: Color,
    name: String,
    //TODO activated doesn't actually work
    activated: Boolean = false,
    animationTime: Float = 0.5f,
    fnToActivate: (state: Boolean) -> Unit
    ): UIContainer() {
    val holder: UIBlock
    val textArea: UIWrappedText

    init {
        constrain {
            width = FillConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        holder = UIBlock().constrain {
            x = SiblingConstraint()
            y = CenterConstraint()

            width = FillConstraint()
            height = (textScale * 9f) + 4.pixels()
        } childOf this

        textArea = UIWrappedText(name, false).constrain {
            x = 2.pixels()
            y = CenterConstraint()

            color = Color(0, 0, 0).toConstraint()
        } childOf holder

        ToggleButton(baseColor, activatedColor, " ", activated, animationTime = animationTime) {
            fnToActivate(it)
        }.constrain {
            x = SiblingConstraint(2f)
            y = CenterConstraint()

            height = (textScale * 9f)
            width = (textScale * 9f)
        } childOf holder
    }
}

fun makeCheckBox(
    name: String,
    value: KMutableProperty0<Boolean>,
    xPadding: Float,
    yPadding: Float,
    makeChildOf: UIComponent): CheckBox {
    return CheckBox(Color(
        150, 150, 150),
        Color(0, 170, 0),
        name,
        value.get()) {
        value.set(it)
    } .constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()
        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf
}