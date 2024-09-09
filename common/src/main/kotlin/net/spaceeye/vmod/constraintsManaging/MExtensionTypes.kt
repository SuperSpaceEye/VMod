package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.constraintsManaging.extensions.NonStrippable
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import net.spaceeye.vmod.utils.Registry

object MExtensionTypes: Registry<MConstraintExtension>(false) {
    init {
        register(RenderableExtension::class)
        register(NonStrippable::class)
    }
}