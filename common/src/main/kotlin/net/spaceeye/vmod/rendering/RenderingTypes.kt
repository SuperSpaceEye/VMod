package net.spaceeye.vmod.rendering

import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.spaceeye.vmod.rendering.types.*
import net.spaceeye.vmod.rendering.types.TimedA2BRenderer
import net.spaceeye.vmod.rendering.types.debug.DebugPointRenderer
import net.spaceeye.vmod.utils.Registry
import kotlin.reflect.KClass

object RenderingTypes: Registry<BaseRenderer>(false) {
    private fun register(clazz: KClass<*>) = register(clazz, Platform.getEnvironment() == Env.CLIENT)

    init {
        register(RopeRenderer::class)
        register(A2BRenderer::class)
        register(TimedA2BRenderer::class)
        register(PhysRopeRenderer::class)
        register(ConeBlockRenderer::class)
        register(PhysgunRayRenderer::class)


        register(DebugPointRenderer::class)
    }
    @JvmStatic fun BaseRenderer.getType() = typeToString(this::class.java)
}