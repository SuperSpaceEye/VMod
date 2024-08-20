package net.spaceeye.vmod.toolgun.modes

import net.spaceeye.vmod.toolgun.modes.state.*
import net.spaceeye.vmod.utils.Registry
import kotlin.reflect.KClass

object ToolgunModes: Registry<BaseMode>() {
    init {
        register(ConnectionMode::class)
        register(RopeMode::class)
        register(HydraulicsMode::class)
        register(PhysRopeMode::class)
        register(SliderMode::class)
        register(SyncRotation::class)

        register(ThrusterMode::class)
        register(GravChangerMode::class)
        register(DisableCollisionsMode::class)
        register(SchemMode::class)
        register(ScaleMode::class)
        register(StripMode::class)
        register(ShipRemoverMode::class)

        ToolgunExtensions
    }

    @JvmStatic
    fun <T: BaseMode> getMode(clazz: KClass<T>) = this.typeToSupplier(clazz)
}