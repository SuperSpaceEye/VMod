package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.server.packs.resources.ResourceManager
import java.io.InputStream

class GIFTexture(): AbstractTexture() {
    var sprites = mutableListOf<SlidingFrameTexture>()
    var width = 0
    var height = 0
    var totalFrames = 0
    var framesPerSprite = 0

    var currentFrame = 0

    fun loadFromStream(stream: InputStream) {
        val textures = GIFReader.readGifToTexturesFaster(stream)
        for (texture in textures) {
            sprites.add(SlidingFrameTexture(texture))
            totalFrames += texture.numFrames
        }
        width = textures[0].frameWidth
        height = textures[0].frameHeight
        framesPerSprite = sprites[0].numFrames
    }

    override fun load(resourceManager: ResourceManager) {}

    override fun close() { sprites.forEach { it.close() } }

    fun advanceTime(delta: Float) {
        val sprite = sprites[currentFrame / framesPerSprite]
        if (!sprite.advanceTime(delta)) return

        currentFrame++
        if (currentFrame < totalFrames) return
        currentFrame = 0
    }

    fun blit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        sprites[currentFrame / framesPerSprite].blit(pose, x, y, uWidth, vHeight)
    }

    fun reset() {
        currentFrame = 0
        sprites.forEach { it.reset() }
    }
}