package net.spaceeye.vsource.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import java.awt.Color

class ToolGunGUI: WindowScreen(ElementaVersion.V5) {
    val components = listOf("Weld", "Rope", "AABB Weld", "Thing 1", "Thing 2", "Thing 3", "Thing 4")

    val mainWindow = UIBlock(Color(200, 200, 200)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90f.percent()
        height = 90f.percent()
    } childOf window

    val scrollComponent = ScrollComponent().constrain {
        x = 2f.percent()
        y = 2f.percent()

        width = 15.percent()
        height = 98.percent()
    } childOf mainWindow

    val settingsComponent = UIBlock(Color(240, 240, 240)).constrain {
        val offset = 20

        x = offset.percent()
        y = 2.percent()

        width = 100.percent() - offset.percent() - 2.percent()
        height = 100.percent() - 4.percent()
    } childOf mainWindow

    init {
        makeScrollComponents()
    }

    fun makeScrollComponents() {
        for ((i, name) in components.withIndex()) {
            val componentColor = if (i % 2 == 0) {
                Color(100, 100, 100)
            } else {
                Color(120, 120, 120)
            }

            val parent = UIBlock(componentColor).constrain {
                x = CenterConstraint()
                y = SiblingConstraint()

                width = 100.percent()
                height = ChildBasedMaxSizeConstraint() + 4.pixels()
            }.onMouseEnter {
                animate {
                    setColorAnimation(
                        Animations.OUT_EXP,
                        0.5f,
                        Color(150, 150, 150).toConstraint()
                    )
                }
            }.onMouseLeave {
                animate {
                    setColorAnimation(
                        Animations.OUT_EXP,
                        0.5f,
                        componentColor.toConstraint()
                    )
                }
            } childOf scrollComponent

            UIText(name, shadow = false).constrain {
                x = 2.pixels()
                y = CenterConstraint()

                textScale = 1.pixels()

                color = Color.BLACK.toConstraint()
            } childOf parent
        }

//        repeat(5) {
//            UIBlock(Color.RED).constrain {
//                x = CenterConstraint()
//                y = SiblingConstraint(padding = 2f)
//
//                width = 50.pixels()
//                height = 25.pixels()
//            } childOf scrollComponent
//        }
    }

//    fun
}