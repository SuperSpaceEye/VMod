package net.spaceeye.vmod.rendering

import net.spaceeye.vmod.rendering.types.*
import net.spaceeye.vmod.utils.Registry

object RenderingTypes: Registry<BaseRenderer>() {
    init {
        register(::RopeRenderer)
        register(::A2BRenderer)
        register(::TimedA2BRenderer)
        register(::PhysRopeRenderer)
    }
}