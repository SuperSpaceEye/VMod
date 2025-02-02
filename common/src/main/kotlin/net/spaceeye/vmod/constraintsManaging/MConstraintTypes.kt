package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.constraintsManaging.types.constraints.*
import net.spaceeye.vmod.constraintsManaging.types.entities.*
import net.spaceeye.vmod.utils.Registry

object MConstraintTypes: Registry<MConstraint>(false) {
    init {
        register(RopeMConstraint::class)
        register(HydraulicsMConstraint::class)
//        register(PhysRopeMConstraint::class)
        register(DisabledCollisionMConstraint::class)
        register(ConnectionMConstraint::class)
        register(SliderMConstraint::class)
        register(SyncRotationMConstraint::class)
        register(GearMConstraint::class)

        register(ThrusterMConstraint::class)
        register(SensorMConstraint::class)
    }
    @JvmStatic inline fun MConstraint.getType() = typeToString(this::class.java)
}