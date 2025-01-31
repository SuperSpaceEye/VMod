package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.constraintsManaging.types.*
import net.spaceeye.vmod.utils.Registry

object MConstraintTypes: Registry<MConstraint>(false) {
    init {
        register(RopeMConstraint::class)
        register(HydraulicsMConstraint::class)
//        register(PhysRopeMConstraint::class)
        register(DisabledCollisionMConstraint::class)
        register(ConnectionMConstraint::class)
        register(ThrusterMConstraint::class)
        register(SliderMConstraint::class)
        register(SyncRotationMConstraint::class)
        register(GearMConstraint::class)
    }
    @JvmStatic inline fun MConstraint.getType() = typeToString(this::class.java)
}