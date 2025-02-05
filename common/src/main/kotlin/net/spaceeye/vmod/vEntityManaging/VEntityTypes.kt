package net.spaceeye.vmod.vEntityManaging

import net.spaceeye.vmod.vEntityManaging.types.constraints.*
import net.spaceeye.vmod.vEntityManaging.types.entities.*
import net.spaceeye.vmod.utils.Registry

object VEntityTypes: Registry<VEntity>(false) {
    init {
        register(RopeConstraint::class)
        register(HydraulicsConstraint::class)
//        register(PhysRopeMConstraint::class)
        register(DisabledCollisionConstraint::class)
        register(ConnectionConstraint::class)
        register(SliderConstraint::class)
        register(SyncRotationConstraint::class)
        register(GearConstraint::class)

        register(ThrusterVEntity::class)
        register(SensorVEntity::class)
    }
    @JvmStatic inline fun VEntity.getType() = typeToString(this::class.java)
}