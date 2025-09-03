package net.spaceeye.vmod.compat.patchouli

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.spaceeye.vmod.GIFReader
import net.spaceeye.vmod.GLMaxTextureSize
import net.spaceeye.vmod.ImageFrame
import net.spaceeye.vmod.MOD_ID
import org.apache.commons.io.output.ByteArrayOutputStream
import vazkii.patchouli.client.book.gui.GuiBook
import vazkii.patchouli.client.book.gui.GuiBookEntry
import vazkii.patchouli.client.book.page.abstr.PageWithText
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

class SlidingFrameTexture(): AbstractTexture() {
    var image: NativeImage? = null
    val delays = mutableListOf<Int>()

    var width = 0
    var height = 0

    var spriteWidth = 0
    var spriteHeight = 0

    var widthTiles = 0
    var heightTiles = 0

    var numFrames = 0
    var currentFrame = 0

    var time = 0f

    fun loadFromStream(frames: Array<ImageFrame>, startFrame: Int): Int {
        width = frames[0].width
        height = frames[0].height

        val remainingFrames = frames.size - startFrame

        val maxWidthTiles = GLMaxTextureSize / width
        val maxHeightTiles = GLMaxTextureSize / height

        var remainderTiles = 0
        widthTiles = if (maxWidthTiles >= remainingFrames) { remainingFrames } else { maxWidthTiles }
        heightTiles = if (maxWidthTiles >= remainingFrames) { 1 } else {
            if (maxWidthTiles * maxHeightTiles >= remainingFrames) {
                remainderTiles = remainingFrames % maxWidthTiles
                remainingFrames / maxWidthTiles + if (remainderTiles == 0) 0 else 1
            } else {
                maxHeightTiles
            }
        }

        spriteWidth = widthTiles * width
        spriteHeight = heightTiles * height

        numFrames = if (remainderTiles == 0) {
            widthTiles * heightTiles
        } else {
            widthTiles * (heightTiles - 1) + remainderTiles
        }

        val sprite = BufferedImage(spriteWidth, spriteHeight, frames[0].image.type)

        for (i in startFrame until numFrames) {
            val frame = frames[i]
            delays.add(frame.delay)

            val x = ((i - startFrame) % widthTiles) * width
            val y = ((i - startFrame) / widthTiles) * height

            sprite.graphics.drawImage(frame.image, x, y, width, height, Color.WHITE, null)
        }

        val inputBytes = ByteArrayOutputStream().also { ImageIO.write(sprite, "png", it) }.toInputStream()
        image = NativeImage.read(inputBytes)

        val image = image!!
        TextureUtil.prepareImage(this.getId(), 0, image.width, image.height)
        image.upload(0, 0, 0, 0, 0, image.width, image.height, false, false, false, true)

        return startFrame + numFrames
    }

    override fun load(resourceManager: ResourceManager?) {}

    override fun close() {
        image!!.close()
        this.releaseId()
    }

    fun setAsShaderTexture() {
        RenderSystem._setShaderTexture(0, this.getId())
    }

    //https://usage.imagemagick.org/anim_basics/
    //if frame was advanced, then true
    fun advanceTime(delta: Float): Boolean {
        time += delta / 10f

        val delay = delays[currentFrame]
        if (time <= delay) return false

        time -= delay
        currentFrame++

        if (currentFrame < numFrames) return true
        currentFrame = 0

        return true
    }

    fun blit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        this.blit(pose, x, y, 0, uWidth, vHeight)
    }

    private fun blit(pose: PoseStack, x: Int, y: Int, blitOffset: Int, uWidth: Int, vHeight: Int) {
        this.innerBlit(pose, x, x + uWidth, y, y + vHeight, blitOffset,
            (width  * (currentFrame % widthTiles + 0)) / spriteWidth.toFloat(),
            (width  * (currentFrame % widthTiles + 1)) / spriteWidth.toFloat(),
            (height * (currentFrame / widthTiles + 0)) / spriteHeight.toFloat(),
            (height * (currentFrame / widthTiles + 1)) / spriteHeight.toFloat()
        )
    }

    private fun innerBlit(pose: PoseStack, x1: Int, x2: Int, y1: Int, y2: Int, blitOffset: Int, minU: Float, maxU: Float, minV: Float, maxV: Float) {
        setAsShaderTexture()
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        val matrix4f = pose.last().pose();
        val bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x1.toFloat(), y1.toFloat(), blitOffset.toFloat()).uv(minU, minV).endVertex();
        bufferBuilder.vertex(matrix4f, x1.toFloat(), y2.toFloat(), blitOffset.toFloat()).uv(minU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, x2.toFloat(), y2.toFloat(), blitOffset.toFloat()).uv(maxU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, x2.toFloat(), y1.toFloat(), blitOffset.toFloat()).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}

class GIFTexture(): AbstractTexture() {
    var sprites = mutableListOf<SlidingFrameTexture>()
    var width = 0
    var height = 0
    var totalFrames = 0
    var framesPerSprite = 0

    var currentFrame = 0

    fun loadFromStream(stream: InputStream) {
        val frames = GIFReader().readGIF(stream)
        totalFrames = frames.size

        width = frames[0].width
        height = frames[0].height

        var framePos = 0
        do {
            sprites.add(SlidingFrameTexture().also { framePos = it.loadFromStream(frames, framePos) })
        } while (framePos < totalFrames)
        framesPerSprite = sprites[0].numFrames
    }

    override fun load(resourceManager: ResourceManager) {}

    override fun close() { sprites.forEach { it.close() } }

    fun advanceTime(delta: Float) {
        val sprite = sprites[currentFrame / framesPerSprite]
        if (!sprite.advanceTime(delta)) return
        if (sprite.currentFrame == 0) {
            //if sprite looped then reset time
            sprite.time = 0f
        }

        currentFrame++
        if (currentFrame < totalFrames) return
        currentFrame = 0
    }

    fun blit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        sprites[currentFrame / framesPerSprite].blit(pose, x, y, uWidth, vHeight)
    }
}

object TEST {
    val test1 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif1.gif")).open()) }
    val test2 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif2.gif")).open()) }
    val test3 = GIFTexture().also { it.loadFromStream(Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/test_gif3.gif")).open()) }
}

class PageGIF: PageWithText() {
    var title: String? = null
    var border = false

    override fun getTextHeight(): Int = 120

    override fun onDisplayed(parent: GuiBookEntry?, left: Int, top: Int) {
        super.onDisplayed(parent, left, top)
        TEST.test3.sprites.forEach { it.time = 0f; it.currentFrame = 0 }
        TEST.test3.currentFrame = 0
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pticks: Float) {
        var x = GuiBook.PAGE_WIDTH / 2 - 53
        var y = 7

        graphics.setColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        graphics.pose().scale(.5f, .5f, .5f)

        //ptick = delta / ms_per_tick
        //ms_per_tick = 50
        TEST.test3.advanceTime(pticks * 50)
        TEST.test3.blit(graphics.pose(), x*2+6, y*2+6, 200, 200)

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