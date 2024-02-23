package net.spaceeye.vsource.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color

//TODO doesn't fucking workj
class DropDown(
    menuName: String,
    items: List<Pair<String, () -> Unit>>,

    text_scale: Float = 1f,
): UIBlock() {
    var activated = false

    init {
        constrain {
            width = FillConstraint()
            height = ChildBasedMaxSizeConstraint() + 2.pixels() + 50.pixels()
            color = Color(255, 0, 0).toConstraint()
        }
        val holder = UIBlock().constrain {
            width = 50.pixels()
            height = 50.pixels()
            x = CenterConstraint()
            y = CenterConstraint()
            color = Color(0, 255, 0).toConstraint()
        } childOf this

        setColor(Color(255, 0, 0))

        val text = UIText(menuName, shadow = false).constrain {
            y = CenterConstraint()
            x = 4.pixels()

            textScale = text_scale.pixels()

            color = Color(0, 0, 0).toConstraint()
        } childOf this

        val itemsHolder = UIBlock().constrain {
            x = CenterConstraint()
            y = (text.getTextScale() * 9f).pixels() + 2.pixels()

            width = FillConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        for((name, fn) in items) {
            val item = UIBlock().constrain {
                x = CenterConstraint()
                y = SiblingConstraint()

                width = FillConstraint()
                height = ChildBasedMaxSizeConstraint()
            }.onMouseClick {
                fn()
            } childOf itemsHolder

            UIText(name, shadow = false).constrain {
                y = CenterConstraint()
                x = CenterConstraint()
                textScale = text_scale.pixels()

                color = Color(0, 0, 0).toConstraint()
            } childOf item
        }

        onMouseClick {
            activated = !activated
            if (activated) {
                itemsHolder childOf this
            } else {
                removeChild(itemsHolder)
            }
        }
    }
}