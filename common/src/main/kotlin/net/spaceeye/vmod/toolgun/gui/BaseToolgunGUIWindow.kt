package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.translate.get
import java.awt.Color

abstract class BaseToolgunGUIWindow(mainWindow: UIBlock): ToolgunWindow {
    protected val scrollComponent = ScrollComponent().constrain {
        x = 2f.percent()
        y = 2f.percent()

        width = 15.percent()
        height = 98.percent()
    } childOf mainWindow

    protected  val settingsComponent = UIBlock(Color(240, 240, 240)).constrain {
        val offset = 20

        x = offset.percent()
        y = 2.percent()

        width = 100.percent() - offset.percent() - 2.percent()
        height = 100.percent() - 4.percent()
    } childOf mainWindow

    protected val settingsScrollComponent = ScrollComponent().constrain {
        x = 0.percent()
        y = 0.percent()

        width = 100.percent()
        height = 100.percent()
    } childOf settingsComponent

    protected fun makeScrollComponents(components: List<GUIBuilder>, mouseClickFn: (component: GUIBuilder) -> Unit) {
        for ((i, component) in components.withIndex()) {
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
            }.onMouseClick {
                mouseClickFn(component)
            } childOf scrollComponent

            UIWrappedText(component.itemName.get(), shadow = false).constrain {
                x = 2.pixels()
                y = CenterConstraint()

                width = 100.percent()

                textScale = 1.pixels()

                color = Color.BLACK.toConstraint()
            } childOf parent
        }
    }
}