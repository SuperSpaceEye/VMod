package net.spaceeye.vsource.gui

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

data class DItem(val name: String, val highlight: Boolean, val fnToApply: () -> Unit)

// TODO refactor this
class DropDown(
    menuName: String,
    dItems: List<DItem>,

    val highlightColor: Color = Color(0, 170, 0),
    text_scale: Float = 1f,
): UIBlock() {
    var activated = false

    var highlightStateOfButtons = mutableListOf<Boolean>()
    var buttons = mutableListOf<Button>()

    fun highlightButtons(highlight: Boolean = true) {
        buttons.zip(highlightStateOfButtons).forEach {
            (it, activated) ->
            when (activated) {
                true -> {
                    it.activeColor = highlightColor
                    it.animate {
                        setColorAnimation(
                            Animations.OUT_EXP,
                            it.animationTime,
                            (if (highlight) {it.activeColor.brighter()} else {it.activeColor}).toConstraint()
                        )
                    }
                }
                false -> {
                    it.activeColor = it.baseColor
                    it.animate {
                        setColorAnimation(
                            Animations.OUT_EXP,
                            it.animationTime,
                            it.activeColor.toConstraint()
                        )
                    }
                }
            }
        }
    }

    fun changeColors(clickedButton: Button) {
        buttons.forEachIndexed { i, it -> highlightStateOfButtons[i] = it == clickedButton }
        highlightButtons()
    }

    init {
        constrain {
            width = 100.percent() - 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()
        }

        var _this = this
        lateinit var itemsHolder: UIComponent

        val menuButton = Button(Color(150, 150, 150), menuName) {
            activated = !activated
            when (activated) {
                true  -> { itemsHolder childOf _this; highlightButtons(false) }
                false -> { _this.removeChild(itemsHolder) }
            }
        }.constrain {
            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()

            x = 2.pixels()
            y = 2.pixels()
        } childOf this

        itemsHolder = UIBlock().constrain {
            y = SiblingConstraint()

            width = 100.percent() - 4.pixels()
            height = ChildBasedSizeConstraint() + 2.pixels()
        }

        var first = true
        for((i, pair) in dItems.withIndex()) {
            val (name, highlight, fn) = pair

            val componentColor = if (i % 2 == 0) {
                Color(120, 120, 120)
            } else {
                Color(150, 150, 150)
            }

            lateinit var itemButton: Button
            itemButton = Button(componentColor, name) {
                fn()
                changeColors(itemButton)
            }.constrain {
                x = 2.pixels()
                y = SiblingConstraint()
                if (first) y += 2.pixels()

                width = 100.percent()
                height = ChildBasedSizeConstraint() + 4.pixels()
            } childOf itemsHolder
            first = false

            highlightStateOfButtons.add(highlight)
            buttons.add(itemButton)
        }
    }
}

fun makeDropDown(name: String,
                 makeChildOf: UIBlock,
                 xPadding: Float,
                 yPadding: Float,
                 dItems: List<DItem>): DropDown {
    val dropDown = DropDown(name, dItems).constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()

        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf

    return dropDown
}