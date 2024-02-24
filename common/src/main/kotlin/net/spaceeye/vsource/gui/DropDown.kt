package net.spaceeye.vsource.gui

import gg.essential.elementa.components.*
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
            width = 100.percent() - 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()
        }

        val textHolder = UIBlock(Color(150, 150, 150)).constrain {
            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()

            x = 2.pixels()
            y = 2.pixels()
        } childOf this

        val text = UIText(menuName, shadow = false).constrain {
            textScale = text_scale.pixels()

            x = CenterConstraint()
            y = CenterConstraint()

            color = Color(0, 0, 0).toConstraint()
        } childOf textHolder

        val itemsHolder = UIBlock().constrain {
            y = SiblingConstraint()

            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedSizeConstraint() + 2.pixels()
        }

        var first = true
        for((i, pair) in items.withIndex()) {
            val (name, fn) = pair

            val componentColor = if (i % 2 == 0) {
                Color(120, 120, 120)
            } else {
                Color(150, 150, 150)
            }

            val item = UIBlock(componentColor).constrain {
                x = 2.pixels()
                y = SiblingConstraint()
                if (first) y += 2.pixels()

                width = ChildBasedMaxSizeConstraint() + 4.pixels()
                height = ChildBasedSizeConstraint() + 4.pixels()
            }.onMouseClick {
                fn()
            } childOf itemsHolder
            first = false

            UIText(name, shadow = false).constrain {
                y = CenterConstraint()
                x = CenterConstraint()
                textScale = text_scale.pixels()

                color = Color(0, 0, 0).toConstraint()
            } childOf item
        }

        val _this = this

        textHolder.onMouseClick {
            activated = !activated
            if (activated) {
                itemsHolder childOf _this
            } else {
                _this.removeChild(itemsHolder)
            }
        }
    }
}

fun makeDropDown(name: String,
                 makeChildOf: UIBlock,
                 xPadding: Float,
                 yPadding: Float,
                 items: List<Pair<String, () -> Unit>>): DropDown {
    val dropDown = DropDown(name, items).constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()

        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf

    return dropDown
}