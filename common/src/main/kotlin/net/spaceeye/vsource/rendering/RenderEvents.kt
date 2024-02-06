package net.spaceeye.vsource.rendering

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

object RenderEvents {
    val WORLD: Event<RenderEvent<PoseStack>> = EventFactory.createLoop()

    fun interface RenderEvent<T> {
        fun rendered(matrixStack: T, buffer: MultiBufferSource, camera: Camera)
    }
}