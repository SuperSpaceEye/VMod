package net.spaceeye.vmod.guiElements

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import java.awt.Color

class Folder(
    buttonName: String,
    expanded: Boolean = false,
    textScale: Float = 1f,
    animationTime: Float = 0.5f,
    baseButtonColor: Color = Color(150, 150, 150),
    activatedButtonColor: Color = Color(0, 170, 0),
    subItemsCreationFn: (UIComponent) -> Unit
): UIBlock() {
    val folderButton: ToggleButton
    val subItemsHolder: UIBlock = UIBlock()
    val unhideItems: (Boolean) -> Unit = {
        if (it) subItemsHolder.unhide() else subItemsHolder.hide()
        recalculateConstrains()
    }

    fun recalculateConstrains() {
        constrain {
            width = 100.percent
            height = ChildBasedSizeConstraint() + 4.pixels()
        }
    }

    init {
        subItemsCreationFn(subItemsHolder)

        folderButton = ToggleButton(baseButtonColor, activatedButtonColor, buttonName, expanded, textScale, animationTime, unhideItems) constrain {
            x = 0.pixels
            y = SiblingConstraint()
        } childOf this

        subItemsHolder constrain {
            x = 0.pixels
            y = SiblingConstraint()

            width = 100.percent
            height = ChildBasedSizeConstraint() + 4.pixels()
        } childOf this

        recalculateConstrains()
    }
}

fun makeFolder(name: String, makeChildOf: UIComponent, xPadding: Float, yPadding: Float, subItemsCreationFn: (UIComponent) -> Unit): Folder {
    return (Folder(name, subItemsCreationFn = subItemsCreationFn) constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()

        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf).also { it.unhideItems(false) }
}