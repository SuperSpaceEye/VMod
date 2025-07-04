package net.spaceeye.vmod.guiElements

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.toConstraint
import gg.essential.elementa.dsl.width
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScrollMenu(
    var maxNumItemsDisplayed: Int = 10,
    color: Color = Color.WHITE,
    var textColor: Color = Color.BLACK,
    var chosenByColor: Color = Color.GREEN
): UIBlock(color) {
    var lastCursorPos = 0

    val allowedSpace get() = maxNumItemsDisplayed - 1

    var top = 0
    var bot = maxNumItemsDisplayed

    init {
        constrain {
            width = 1.pixels
            height = (maxNumItemsDisplayed * 2).pixels
        }
    }

    val scrollComponent = ScrollComponent() constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    fun updateHighlightedOption(cursorPos: Int, cb: (Int) -> Unit = {}) {
        if (scrollComponent.allChildren.isEmpty) {return}
        val numItems = scrollComponent.allChildren.size

        val chosenPos = max(min(cursorPos, numItems - 1), 0)

        scrollComponent.allChildren[lastCursorPos].constrain { color = textColor.toConstraint() }
        scrollComponent.allChildren[chosenPos].constrain { color = chosenByColor.toConstraint() }


        val newTop = max(min(cursorPos - (maxNumItemsDisplayed - allowedSpace), numItems - 1), 0)
        val newBot = max(min(cursorPos + (maxNumItemsDisplayed - allowedSpace), numItems - 1), 0)
        val diff = if (newTop < top) {
            newTop - top
        } else if (newBot > bot) {
            newBot - bot
        } else 0

        if (abs(diff) > 0) {
            val offset = scrollComponent.allChildren.maxOf { it.getHeight() + 2f } * diff
            scrollComponent.scrollTo(verticalOffset = scrollComponent.verticalOffset - offset)

            bot += diff
            top += diff
        }

        lastCursorPos = chosenPos

        cb(chosenPos)
    }

    fun setItems(items: List<String>) {
        scrollComponent.clearChildren()
        lastCursorPos = 0
        top = 0
        bot = maxNumItemsDisplayed - 1

        val maxHeight = items.map {
            UIText(it, false) constrain {
                color = textColor.toConstraint()
                x = 2.pixels
                y = SiblingConstraint(1f) + 1.pixels
            } childOf scrollComponent
        }.let{ if (it.isNotEmpty()) it.maxBy { it.getHeight() }.getHeight() else 0f}

        val maxWidth = if (items.isNotEmpty()) items.maxBy { it.length }.width() else 0

        this constrain {
            width = maxWidth.pixels + 4.pixels
            height = (min(maxNumItemsDisplayed, items.size) * (maxHeight + 2)).pixels
        }

        if (items.isNotEmpty()) {
            scrollComponent.scrollTo(verticalOffset = 0f)
        }
    }
}