package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.addChild
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.*
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.toConstraint
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.state.VEntityChanger
import net.spaceeye.vmod.utils.separateTypeName
import net.spaceeye.vmod.vEntityManaging.VEntityTypes.getType
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object VEntityChangerWorldMenu: ScreenWindowAddition {
    private var menuContainer: UIContainer = UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 100.percent()
        height = 100.percent()
    }

    var itemsHolder: UIBlock = UIBlock(Color(50, 50, 50, 127))

    val scrollComponent = ScrollComponent().constrain {
        x = 0.pixels
        y = 0.pixels

        width = 100.percent
        height = 100.percent
    }

    override fun init(screenContainer: UIContainer) {
        itemsHolder.hide()

        menuContainer childOf screenContainer
        itemsHolder childOf menuContainer
        scrollComponent childOf itemsHolder
    }

    val lastIds = mutableSetOf<Int>()
    var lastCursorPos = 0
    var maxNumItemsDisplayed = 10

    val allowedSpace get() = maxNumItemsDisplayed - 1

    var top = 0
    var bot = maxNumItemsDisplayed

    private fun updateHighlightedOption() {
        if (scrollComponent.allChildren.isEmpty) {return}
        var cursorPos = VEntityChanger.cursorPos
        val numItems = scrollComponent.allChildren.size

        val chosenPos = max(min(cursorPos, numItems-1), 0)

        scrollComponent.allChildren[lastCursorPos].constrain { color = Color(255, 255, 255).toConstraint() }
        scrollComponent.allChildren[chosenPos].constrain { color = Color(0, 255, 0).toConstraint() }


        val newTop = max(min(cursorPos - (maxNumItemsDisplayed - allowedSpace), numItems-1), 0)
        val newBot = max(min(cursorPos + (maxNumItemsDisplayed - allowedSpace), numItems-1), 0)
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

        val rID = VEntityChanger.clientVEntities[chosenPos].second
        ClientRenderingData.getItem(rID)?.also { it.highlightUntilRenderingTicks(it.renderingTick + 3) }
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        itemsHolder.unhide()

        if (   !ToolgunItem.playerIsUsingToolgun()
            || ClientToolGunState.currentMode !is VEntityChanger) {return itemsHolder.hide()}

        val curIds = VEntityChanger.clientVEntities.map { it.first.mID }.toSet()
        if (   VEntityChanger.clientVEntities.isEmpty()
            || (scrollComponent.allChildren.isNotEmpty() && (!curIds.containsAll(lastIds) || !lastIds.containsAll(curIds)))
            ) {
            scrollComponent.clearChildren()
            itemsHolder.hide()
            lastIds.clear()
            lastCursorPos = 0
            top = 0
            bot = maxNumItemsDisplayed - 1
            scrollComponent.scrollTo(verticalOffset = 0f)
            return
        }
        updateHighlightedOption()
        if (curIds.containsAll(lastIds) && lastIds.containsAll(curIds)) {return}
        lastIds.clear()
        lastIds.addAll(curIds)

        val names = VEntityChanger.clientVEntities.mapIndexed { i, it -> "${i+1}. ${separateTypeName(it.first.getType())}" }
        val sizes = names.map {
            UIText(it).constrain {
                color = Color.WHITE.toConstraint()
                x = 2.pixels
                y = SiblingConstraint(1f) + 1f.pixels
            }.also { text -> scrollComponent.addChild { text }
            }.let { Pair(it.getWidth(), it.getHeight()) }
        }

        itemsHolder.constrain {
            x = 50.percent + 4.pixels
            y = 50.percent + 4.pixels

            width  = (sizes.maxOf { it.first } + 4f).pixels
            height = ((sizes.maxOf { it.second + 2f }) * min(maxNumItemsDisplayed, sizes.size)).pixels
        }

        itemsHolder.unhide()
    }
}