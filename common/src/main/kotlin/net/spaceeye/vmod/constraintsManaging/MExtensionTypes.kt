package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.constraintsManaging.extensions.*
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import net.spaceeye.vmod.utils.Registry

object MExtensionTypes: Registry<MConstraintExtension>(false) {
    init {
        register(RenderableExtension::class)
        register(Strippable::class)
        register(SignalActivator::class)
    }
}