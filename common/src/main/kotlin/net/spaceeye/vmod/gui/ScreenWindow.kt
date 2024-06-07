package net.spaceeye.vmod.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent

class ScreenWindow: WindowScreen(ElementaVersion.V5, drawDefaultBackground = false) {
    val screenContainer = UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 100.percent()
        height = 100.percent()
    } childOf window
}