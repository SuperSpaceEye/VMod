package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsTypes
import net.spaceeye.vmod.translate.get
import java.awt.Color

class ServerSettingsGUI(val mainWindow: UIBlock): ToolgunWindow {
    val scrollComponent = ScrollComponent().constrain {
        x = 2f.percent()
        y = 2f.percent()

        width = 15.percent()
        height = 98.percent()
    }

    val settingsComponent = UIBlock(Color(240, 240, 240)).constrain {
        val offset = 20

        x = offset.percent()
        y = 2.percent()

        width = 100.percent() - offset.percent() - 2.percent()
        height = 100.percent() - 4.percent()
    }

    val settingsScrollComponent = ScrollComponent().constrain {
        x = 0.percent()
        y = 0.percent()

        width = 100.percent()
        height = 100.percent()
    } childOf settingsComponent

    fun makeScrollComponents(components: List<GUIBuilder>) {
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
                settingsScrollComponent.clearChildren()
                component.makeGUISettings(settingsScrollComponent)
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

    override fun onGUIOpen() {
        mainWindow.clearChildren()

        ServerToolGunState.checkIfIHaveAccess(ServerToolGunState.AccessTo.ServerSettings) {
            if (!it) {
                UIText("You don't have access to this", false) constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()

                    color = Color.BLACK.toConstraint()
                } childOf mainWindow
                return@checkIfIHaveAccess
            }

            scrollComponent childOf mainWindow
            settingsComponent childOf mainWindow
        }
    }

    init {
        onGUIOpen()
        makeScrollComponents(ServerSettingsTypes.asList().map { it.get() })
    }
}