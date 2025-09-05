package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.utils.Vector3d
import java.io.InputStream
import kotlin.concurrent.thread

class AnimatedGIFTexture(val gif: GIFTexture): AutoCloseable {
    var currentFrame: Int = 0
    var time = 0f

    //https://usage.imagemagick.org/anim_basics/
    //if frame was advanced, then true
    fun advanceTime(delta: Float): Boolean {
        if (!gif.loaded) return false

        time += delta / 10f
        val delay = gif.sprites[currentFrame / gif.framesPerSprite].delays[currentFrame % gif.framesPerSprite]
        if (time <= delay) return false

        time = 0f

        currentFrame++
        if (currentFrame < gif.totalFrames) return true
        currentFrame = 0

        return true
    }

    fun blit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        gif.blit(pose, currentFrame, x, y, uWidth, vHeight)
    }

    fun draw(pose: PoseStack, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
        gif.draw(pose, currentFrame, lu, ld, rd, ru)
    }

    override fun close() {
        gif.close()
    }

    fun reset() {
        currentFrame = 0
        time = 0f
    }
}

class GIFTexture(): AbstractTexture() {
    var sprites = mutableListOf<SlidingFrameTexture>()
    var width = 0
    var height = 0
    var totalFrames = 0
    var framesPerSprite = 0

    var loaded = false

    //TODO rework to have a worker, and it will give job to worker on a single thread
    fun loadFromStream(stream: InputStream) = thread {
        val textures = GIFReader.readGifToTexturesFaster(stream)
        for (texture in textures) {
            sprites.add(SlidingFrameTexture(texture))
            totalFrames += texture.numFrames
        }
        width = textures[0].frameWidth
        height = textures[0].frameHeight
        framesPerSprite = sprites[0].numFrames

        loaded = true
    }

    override fun load(resourceManager: ResourceManager) {}

    override fun close() {
        //TODO stupid
        if (!loaded) {
            thread {
                while (!loaded) {
                    Thread.sleep(1000)
                }
                sprites.forEach { it.close() }
            }
            return
        }
        sprites.forEach { it.close() }
    }

    fun blit(pose: PoseStack, frameNum: Int, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        if (!loaded) { return tempBlit(pose, x, y, uWidth, vHeight) }
        sprites[frameNum / framesPerSprite].blit(pose, frameNum, x, y, uWidth, vHeight)
    }

    fun draw(pose: PoseStack, frameNum: Int, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
        if (!loaded) { return tempDraw(pose, lu, ld, rd, ru) }
        sprites[frameNum / framesPerSprite].draw(pose, frameNum, lu, ld, rd, ru)
    }

    fun animated(): AnimatedGIFTexture = AnimatedGIFTexture(this)

    companion object {
        private val tempTextureLocation = ResourceLocation(MOD_ID, "textures/misc/loading.png")

        private fun tempBlit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
            RenderSystem.setShaderTexture(0, tempTextureLocation)
            SlidingFrameTexture.innerDraw(
                pose,
                Vector3d(x,          y,           0),
                Vector3d(x,          y + vHeight, 0),
                Vector3d(x + uWidth, y + vHeight, 0),
                Vector3d(x + uWidth, y          , 0),
                0f, 1f, 0f, 1f
            )
        }

        private fun tempDraw(pose: PoseStack, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
            RenderSystem.setShaderTexture(0, tempTextureLocation)
            SlidingFrameTexture.innerDraw(pose, lu, ld, rd, ru, 0f, 1f, 0f, 1f)
        }
    }
}