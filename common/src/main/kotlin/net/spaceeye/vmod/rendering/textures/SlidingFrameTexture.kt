package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.server.packs.resources.ResourceManager

class SlidingFrameTexture(
    texture: GIFReader.NativeTextureWithData
): AbstractTexture() {
    var image = texture.image
    var delays = texture.delays

    var width = texture.frameWidth
    var height = texture.frameHeight

    var spriteWidth = texture.spriteWidth
    var spriteHeight = texture.spriteHeight

    var widthTiles = texture.widthTiles
    var heightTiles = texture.heightTiles

    var numFrames = texture.numFrames
    var currentFrame = 0

    var time = 0f

    init {
        TextureUtil.prepareImage(this.getId(), 0, image.width, image.height)
        image.upload(0, 0, 0, 0, 0, image.width, image.height, false, false, false, true)
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

        time = 0f
        currentFrame++

        if (currentFrame < numFrames) return true
        currentFrame = 0

        return true
    }

    fun reset() {
        time = 0f
        currentFrame = 0
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
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        val matrix4f = pose.last().pose();
        val bufferBuilder = Tesselator.getInstance().getBuilder()
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x1.toFloat(), y1.toFloat(), blitOffset.toFloat()).uv(minU, minV).endVertex();
        bufferBuilder.vertex(matrix4f, x1.toFloat(), y2.toFloat(), blitOffset.toFloat()).uv(minU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, x2.toFloat(), y2.toFloat(), blitOffset.toFloat()).uv(maxU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, x2.toFloat(), y1.toFloat(), blitOffset.toFloat()).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}