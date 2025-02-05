package net.spaceeye.vmod.vEntityManaging

import net.spaceeye.vmod.vEntityManaging.extensions.*
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import net.spaceeye.vmod.utils.Registry

object VEExtensionTypes: Registry<VEntityExtension>(false) {
    init {
        register(RenderableExtension::class)
        register(Strippable::class)
        register(SignalActivator::class)
    }
}