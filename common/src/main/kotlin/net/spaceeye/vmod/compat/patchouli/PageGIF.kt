package net.spaceeye.vmod.compat.patchouli

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.rendering.textures.GIFTexture
import vazkii.patchouli.client.book.gui.GuiBook
import vazkii.patchouli.client.book.gui.GuiBookEntry
import vazkii.patchouli.client.book.page.abstr.PageWithText

object TEST {
    var test1 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif1.gif")).open()) }
    var test2 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif2.gif")).open()) }
    var test3 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif3.gif")).open()) }
}

class PageGIF: PageWithText() {
    var title: String? = null
    var border = false

    override fun getTextHeight(): Int = 120

    override fun onDisplayed(parent: GuiBookEntry?, left: Int, top: Int) {
        super.onDisplayed(parent, left, top)
        TEST.test1.reset()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pticks: Float) {
//        TEST.test1.close()
//        TEST.test1 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif2.gif")).open()) }
        var x = GuiBook.PAGE_WIDTH / 2 - 53
        var y = 7

        graphics.setColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        graphics.pose().scale(.5f, .5f, .5f)

        //ptick = delta / ms_per_tick
        //ms_per_tick = 50
        TEST.test1.advanceTime(pticks * 50)
        TEST.test1.blit(graphics.pose(), x*2+6, y*2+6, 200, 200)

        graphics.pose().scale(2f, 2f, 2f)

        if (border) {
            GuiBook.drawFromTexture(graphics, book, x, y, 405, 149, 106, 106)
        }
        if (title != null && title!!.isNotEmpty()) {
            parent.drawCenteredStringNoShadow(graphics, i18n(title), GuiBook.PAGE_WIDTH / 2, -3, book.headerColor)
        }

        super.render(graphics, mouseX, mouseY, pticks)
    }
}