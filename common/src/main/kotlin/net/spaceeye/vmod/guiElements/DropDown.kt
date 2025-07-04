package net.spaceeye.vmod.guiElements

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

data class DItem(val name: String, val highlight: Boolean, val fnToApply: () -> Unit)

class DropDown(
    menuName: String,
    dItems: List<DItem>,

    val activatedColor: Color = Color(0, 170, 0),
    text_scale: Float = 1f,
    val onClose: () -> Unit = {},
    val onOpen: () -> Unit = {}
): UIBlock() {
    lateinit var dropDownButton: Button

    var activated = false

    var activatedStateOfButtons = mutableListOf<Boolean>()
    var buttons = mutableListOf<Button>()

    fun activateButtons(activate: Boolean = true) {
        buttons.zip(activatedStateOfButtons).forEach {
            (it, activated) ->
            when (activated) {
                true -> {
                    it.activeColor = activatedColor
                    it.animate {
                        setColorAnimation(
                            Animations.OUT_EXP,
                            it.animationTime,
                            (if (activate) {it.activeColor.brighter()} else {it.activeColor}).toConstraint()
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

    // is called when any item button was pressed
    private fun changeColors(clickedButton: Button) {
        buttons.forEachIndexed { i, it -> activatedStateOfButtons[i] = it == clickedButton }
        activateButtons()
    }

    init {
        val menuWidth = menuName.width() + 8
        constrain {
            width = menuWidth.pixels
            height = ChildBasedSizeConstraint() + 4.pixels()
        }

        val _this = this
        lateinit var itemsHolder: UIComponent

        val largestName = dItems.maxBy { it.name.length }.name
        val largestWidth = max(largestName.width(text_scale), menuName.width()) + 8

        dropDownButton = Button(Color(150, 150, 150), menuName, text_scale) {
            activated = !activated
            when (activated) {
                true  -> { this.constrain { width = largestWidth.pixels }; itemsHolder childOf _this; activateButtons(false); onOpen() }
                false -> { this.constrain { width = menuWidth   .pixels }; _this.removeChild(itemsHolder); onClose() }
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
            itemButton = Button(componentColor, name, text_scale) {
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

            activatedStateOfButtons.add(highlight)
            buttons.add(itemButton)
        }
    }
}

fun makeDropDown(name: String,
                 makeChildOf: UIComponent,
                 xPadding: Float,
                 yPadding: Float,
                 dItems: List<DItem>): DropDown {
    val dropDown = DropDown(name, dItems).constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()
    } childOf makeChildOf

    return dropDown
}

fun makeDropDown(name: String,
                 value: KMutableProperty0<Enum<*>>,
                 xPadding: Float,
                 yPadding: Float,
                 makeChildOf: UIComponent): DropDown {
    val enumItem = value.get()
    val items = enumItem.declaringJavaClass.enumConstants.map {
        DItem(it.name, it == enumItem) {value.set(it)}
    }
    return makeDropDown(name, makeChildOf, xPadding, yPadding, items)
}