package net.spaceeye.vsource.rendering

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3d
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

object RenderEvents {
    val WORLD: Event<RenderEvent<PoseStack>> = EventFactory.createLoop()

    val WORLD_EVENTS: Event<RenderEventPool<PoseStack>> = EventFactory.createLoop()

    fun interface RenderEvent<T> {
        fun rendered(matrixStack: T, camera: Camera)
    }

//    fun t() {
//        WORLD_EVENTS.register (
//            object : RenderEventPool<PoseStack>() {
//                override fun render(matrixStack: PoseStack, buffer: MultiBufferSource, camera: Camera) {
//                    eventLoop.invoker().rendered(matrixStack, buffer, camera)
//                }
//            }
//        )
//    }

    abstract class RenderEventPool<T> {
        val eventLoop: Event<RenderEvent<T>> = EventFactory.createLoop()

        abstract fun render(matrixStack: T, buffer: MultiBufferSource, camera: Camera)
    }
}