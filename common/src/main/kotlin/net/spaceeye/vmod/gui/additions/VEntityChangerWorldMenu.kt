package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.*
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.guiElements.ScrollMenu
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.toolgun.modes.state.VEntityChanger
import net.spaceeye.vmod.utils.separateTypeName
import net.spaceeye.vmod.vEntityManaging.VEntityTypes.getType
import java.awt.Color

object VEntityChangerWorldMenu: ScreenWindowAddition() {
    private val scrollMenu = ScrollMenu(10, Color(50, 50, 50, 127), Color.WHITE)

    override fun init(screenContainer: UIContainer) {
        scrollMenu.hide()
        scrollMenu childOf screenContainer
    }

    val lastIds = mutableSetOf<Int>()

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        if (!instance.client.playerIsUsingToolgun() || instance.client.currentMode !is VEntityChanger) {return scrollMenu.hide()}
        if (scrollMenu.scrollComponent.allChildren.isNotEmpty()) {
            scrollMenu.unhide()
        }

        val curIds = VEntityChanger.clientVEntities.map { it.first.mID }.toSet()
        if (   VEntityChanger.clientVEntities.isEmpty()
            || (scrollMenu.scrollComponent.allChildren.isNotEmpty() && (!curIds.containsAll(lastIds) || !lastIds.containsAll(curIds)))
            ) {
            scrollMenu.hide()
            scrollMenu.setItems(emptyList())
            lastIds.clear()
            return
        }
        scrollMenu.updateHighlightedOption(VEntityChanger.cursorPos) {
            val rID = VEntityChanger.clientVEntities[it].second
            RenderingData.client.getItem(rID)?.also { it.highlightUntilRenderingTicks(it.renderingTick + 3) }
        }
        if (curIds.containsAll(lastIds) && lastIds.containsAll(curIds)) {return}
        lastIds.clear()
        lastIds.addAll(curIds)
        scrollMenu.hide()

        val names = VEntityChanger.clientVEntities.mapIndexed { i, it -> "${i+1}. ${separateTypeName(it.first.getType())}" }
        scrollMenu.setItems(names)

        scrollMenu.constrain {
            x = 50.percent + 4.pixels
            y = 50.percent + 4.pixels
        }

        scrollMenu.unhide()
    }
}