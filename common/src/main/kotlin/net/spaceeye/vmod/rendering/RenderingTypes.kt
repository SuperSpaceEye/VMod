package net.spaceeye.vmod.rendering

import net.spaceeye.vmod.rendering.types.*
import net.spaceeye.vmod.rendering.types.TimedA2BRenderer
import net.spaceeye.vmod.utils.Registry

object RenderingTypes: Registry<BaseRenderer>(false) {
    init {
        register(RopeRenderer::class)
        register(A2BRenderer::class)
        register(TimedA2BRenderer::class)
        register(PhysRopeRenderer::class)
        register(ConeBlockRenderer::class)
        register(PhysgunRayRenderer::class)
    }
    @JvmStatic inline fun BaseRenderer.getType() = typeToString(this::class.java)
}