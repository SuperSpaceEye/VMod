package net.spaceeye.vmod.guiElements

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

class Button(var baseColor: Color, buttonName: String, text_scale: Float = 1f, var animationTime: Float = 0.5f, fnToActivate: () -> Unit): UIBlock(baseColor) {
    var activeColor = baseColor
    init {
        constrain {
            width = ChildBasedMaxSizeConstraint() + 4.pixels()
            height = ChildBasedMaxSizeConstraint() + 4.pixels()
        }
        onMouseEnter {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().toConstraint()
                )
            }
        }
        onMouseLeave {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.toConstraint()
                )
            }
        }
        onMouseClick {
            fnToActivate()
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().brighter().toConstraint()
                )
            }
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().toConstraint()
                )
            }
        }

         UIText(buttonName, shadow = false).constrain {
            textScale = text_scale.pixels()

            x = CenterConstraint()
            y = CenterConstraint()

            color = Color(0, 0, 0).toConstraint()
        } childOf this
    }
}

class ToggleButton(
    var baseColor: Color,
    var activatedColor: Color,
    buttonName: String, defaultState:
    Boolean, text_scale: Float = 1f,
    var animationTime: Float = 0.5f,
    fnToActivate: (state: Boolean) -> Unit
): UIBlock(baseColor) {
    var activeColor = baseColor
    var state = defaultState
    init {
        onMouseEnter {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().toConstraint()
                )
            }
        }
        onMouseLeave {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.toConstraint()
                )
            }
        }
        onMouseClick {
            state = !state
            activeColor = when(state) {
                true -> activatedColor
                false -> baseColor
            }

            fnToActivate(state)
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().brighter().toConstraint()
                )
            }
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    animationTime,
                    activeColor.brighter().toConstraint()
                )
            }
        }

        UIText(buttonName, shadow = false).constrain {
            textScale = text_scale.pixels()

            x = CenterConstraint()
            y = CenterConstraint()

            color = Color(0, 0, 0).toConstraint()
        } childOf this
    }
}