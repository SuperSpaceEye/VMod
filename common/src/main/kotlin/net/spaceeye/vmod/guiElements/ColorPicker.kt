package net.spaceeye.vmod.guiElements

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.limits.IntLimit
import java.awt.Color

class ColorPicker(defaultColor: Color, val onColorChange: (color: Color) -> Unit): UIBlock() {
    val colorResult = UIBlock(defaultColor)

    private var inColor = Color(defaultColor.red, defaultColor.green, defaultColor.blue, defaultColor.alpha)

    private var r: Int get() {return inColor.red  } set(value) {inColor = Color(value, g, b, a); updateColor()}
    private var g: Int get() {return inColor.green} set(value) {inColor = Color(r, value, b, a); updateColor()}
    private var b: Int get() {return inColor.blue } set(value) {inColor = Color(r, g, value, a); updateColor()}
    private var a: Int get() {return inColor.alpha} set(value) {inColor = Color(r, g, b, value); updateColor()}

    private val xPad = 0.0f
    private val yPad = 0.0f

    var outColor: Color
        get() = inColor
        set(value) {
            r = value.red
            g = value.green
            b = value.blue
            a = value.alpha
        }

    val redTextbox   = makeTextEntry("R: ", ::r , xPad, yPad, this, IntLimit(0, 255))
    val greenTextbox = makeTextEntry("G: ", ::g , xPad, yPad, this, IntLimit(0, 255))
    val blueTextbox  = makeTextEntry("B: ", ::b , xPad, yPad, this, IntLimit(0, 255))
    val alphaTextbox = makeTextEntry("A: ", ::a , xPad, yPad, this, IntLimit(0, 255))

    fun updateColor() {
        colorResult.setColor(inColor)
        onColorChange(inColor)
    }

    init {
        constrain {
            width  = 100.percent()
            height = 100.percent()
        }

        redTextbox   constrain { width = 50.percent() }
        greenTextbox constrain { width = 50.percent() }
        blueTextbox  constrain { width = 50.percent() }
        alphaTextbox constrain { width = 50.percent() }

        colorResult constrain {
            x = SiblingConstraint()
            y = 0.percent()

            width = 50.percent()
            height = 50.percent()
        } childOf this
    }
}