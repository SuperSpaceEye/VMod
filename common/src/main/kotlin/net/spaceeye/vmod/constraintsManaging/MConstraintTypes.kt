package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.constraintsManaging.types.*
import net.spaceeye.vmod.utils.Registry

object MConstraintTypes: Registry<MConstraint>() {
    init {
        register(::RopeMConstraint)
        register(::HydraulicsMConstraint)
        register(::PhysRopeMConstraint)
        register(::DisabledCollisionMConstraint)
        register(::ConnectionMConstraint)
        register(::WinchMConstraint)
        register(::ThrusterMConstraint)
        register(::SliderMConstraint)
    }
}