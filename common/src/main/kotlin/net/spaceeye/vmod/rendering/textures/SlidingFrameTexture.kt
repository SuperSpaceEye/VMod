package net.spaceeye.vmod.rendering.textures

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
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d

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

    init {
        RenderSystem.recordRenderCall {
            TextureUtil.prepareImage(this.getId(), 0, image.width, image.height)
            image.upload(0, 0, 0, 0, 0, image.width, image.height, false, false, false, true)
        }
    }

    override fun load(resourceManager: ResourceManager?) {}

    override fun close() {
        image.close()
        this.releaseId()
    }

    fun setAsShaderTexture() {
        RenderSystem._setShaderTexture(0, this.getId())
    }

    fun blit(pose: PoseStack, frameNum: Int, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        setAsShaderTexture()
        innerDraw(pose,
            Vector3d(x,          y,           0),
            Vector3d(x,          y + vHeight, 0),
            Vector3d(x + uWidth, y + vHeight, 0),
            Vector3d(x + uWidth, y          , 0),
            (width  * (frameNum % widthTiles + 0)) / spriteWidth.toFloat(),
            (width  * (frameNum % widthTiles + 1)) / spriteWidth.toFloat(),
            (height * (frameNum / widthTiles + 0)) / spriteHeight.toFloat(),
            (height * (frameNum / widthTiles + 1)) / spriteHeight.toFloat()
        )
    }

    fun draw(pose: PoseStack, frameNum: Int, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
        setAsShaderTexture()
        innerDraw(pose, lu, ld, rd, ru,
            (width  * (frameNum % widthTiles + 0)) / spriteWidth.toFloat(),
            (width  * (frameNum % widthTiles + 1)) / spriteWidth.toFloat(),
            (height * (frameNum / widthTiles + 0)) / spriteHeight.toFloat(),
            (height * (frameNum / widthTiles + 1)) / spriteHeight.toFloat()
        )
    }

    companion object {
        fun innerDraw(pose: PoseStack, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, minU: Float, maxU: Float, minV: Float, maxV: Float) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            val matrix4f = pose.last().pose()
            val bufferBuilder = Tesselator.getInstance().builder
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            RenderingUtils.Quad.drawQuad(bufferBuilder, matrix4f, 255, 255, 255, 255, 0, 0, lu, ld, rd, ru, minU, maxU, minV, maxV)
            BufferUploader.drawWithShader(bufferBuilder.end())
        }
    }
}