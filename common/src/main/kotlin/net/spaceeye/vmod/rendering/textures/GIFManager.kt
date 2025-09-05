package net.spaceeye.vmod.rendering.textures

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.OnFinalize
import net.spaceeye.vmod.utils.MPair

object GIFManager {
    /**
     * On being collected by GC will decrease ref counter of texture. Uses finalize which is probably not great but whatever
     */
    class TextureReference(val id: String, var gif: AnimatedGIFTexture, private val wasFinalized: () -> Unit): OnFinalize() {
        override fun onFinalize() {
            wasFinalized()
        }
    }

    private val storage = mutableMapOf<String, MPair<Int, GIFTexture>>()
    private val resourceManager = Minecraft.getInstance().resourceManager

    private fun makeRef(id: String, pair: MPair<Int, GIFTexture>): TextureReference {
        pair.first++
        return TextureReference(id, pair.second.animated()) {
            pair.first--
            if (pair.first <= 0) {
                pair.second.close()
                storage.remove(id)
            }
        }
    }

    /**
     * SAVE REFERENCE, DO NOT SAVE TEXTURE ITSELF.
     */
    fun getTextureFromLocation(location: ResourceLocation): TextureReference {
        val strId = location.toString()
        storage[strId]?.also { return makeRef(strId, it) }

        val stream = resourceManager.getResourceOrThrow(location).open()
        val texture = GIFTexture().also{it.loadFromStream(stream)}

        return makeRef(strId, MPair(0, texture).also { storage[strId] = it })
    }
}