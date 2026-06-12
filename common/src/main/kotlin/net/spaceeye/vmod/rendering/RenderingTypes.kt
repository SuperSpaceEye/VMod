package net.spaceeye.vmod.rendering

import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.spaceeye.vmod.rendering.types.*
import net.spaceeye.vmod.rendering.types.debug.*
import net.spaceeye.vmod.rendering.types.deprecated.*
import net.spaceeye.vmod.utils.Registry
import kotlin.reflect.KClass

object RenderingTypes: Registry<BaseRenderer>(false) {
    private fun register(clazz: KClass<*>) = register(clazz, Platform.getEnvironment() == Env.CLIENT)

    init {
        register(RopeRenderer::class)
        register(A2BRenderer::class)
        register(TimedA2BRenderer::class)
        register(PhysRopeRenderer::class)
        register(BlockStateRenderer::class)
        register(PhysgunRayRenderer::class)
        register(TubeRopeRenderer::class)
        register(PhysEntityBlockRenderer::class)
        register(A2BRendererAnimated::class)
        register(ComplexBodyRenderer::class); ComplexBodyRenderer

        register(DebugPointRenderer::class)
        register(RainbowRenderer::class)

        @Suppress("DEPRECATION_ERROR")
        register(ConeBlockRenderer::class)
    }
    @JvmStatic fun BaseRenderer.getType() = typeToString(this::class.java)
}