package net.spaceeye.vmod.toolgun.modes

import dev.architectury.platform.Platform
import net.spaceeye.vmod.toolgun.PlayerAccessManager
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
//        register(GearMode::class)

        register(ThrusterMode::class)
        register(SensorMode::class)
        register(GravChangerMode::class)
        register(DisableCollisionsMode::class)
        register(SchemMode::class)
        register(ScaleMode::class)
        register(StripMode::class)
        register(ShipRemoverMode::class)
        register(MassChangerMode::class)
//        register(COMChangerMode::class)
        register(VEntityChanger::class)
        if (Platform.isDevelopmentEnvironment()) {
            register(TestMode::class)
            register(FunnyMode::class)
            register(IdkMode::class)
        }

        ToolgunExtensions
        initAccessPermissions()
    }

    private fun initAccessPermissions() {
        asTypesList().forEach {
            PlayerAccessManager.addPermission("Allow ${it.simpleName}")
        }
    }

    @JvmStatic
    fun Class<BaseMode>.getPermission() = "Allow ${this.simpleName}"
}