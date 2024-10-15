package net.spaceeye.vmod.rendering

import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.spaceeye.vmod.rendering.types.*
import net.spaceeye.vmod.rendering.types.TimedA2BRenderer
import net.spaceeye.vmod.utils.Registry

object RenderingTypes: Registry<BaseRenderer>(false) {
    init {
        register(RopeRenderer::class,       Platform.getEnvironment() == Env.CLIENT)
        register(A2BRenderer::class,        Platform.getEnvironment() == Env.CLIENT)
        register(TimedA2BRenderer::class,   Platform.getEnvironment() == Env.CLIENT)
        register(PhysRopeRenderer::class,   Platform.getEnvironment() == Env.CLIENT)
        register(ConeBlockRenderer::class,  Platform.getEnvironment() == Env.CLIENT)
        register(PhysgunRayRenderer::class, Platform.getEnvironment() == Env.CLIENT)
    }
    @JvmStatic inline fun BaseRenderer.getType() = typeToString(this::class.java)
}