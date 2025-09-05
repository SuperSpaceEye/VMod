package net.spaceeye.vmod.compat.patchouli

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.rendering.textures.GIFManager
import vazkii.patchouli.client.book.gui.GuiBook
import vazkii.patchouli.client.book.gui.GuiBookEntry
import vazkii.patchouli.client.book.page.abstr.PageWithText

class PageGIF: PageWithText() {
    var title: String? = null
    var border = false
    //TODO it seems like this is loaded at the book creation, which is not great
    val gifRef = GIFManager.getTextureFromLocation(ResourceLocation(MOD_ID, "textures/gif/test_gif2.gif"))

    override fun getTextHeight(): Int = 120

    override fun onDisplayed(parent: GuiBookEntry?, left: Int, top: Int) {
        super.onDisplayed(parent, left, top)
        gifRef.gif.reset()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pticks: Float) {
        var x = GuiBook.PAGE_WIDTH / 2 - 53
        var y = 7

        graphics.setColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        graphics.pose().scale(.5f, .5f, .5f)

        //ptick = delta / ms_per_tick
        //ms_per_tick = 50
        gifRef.gif.advanceTime(pticks * 50)
        gifRef.gif.blit(graphics.pose(), x*2+6, y*2+6, 200, 200)

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