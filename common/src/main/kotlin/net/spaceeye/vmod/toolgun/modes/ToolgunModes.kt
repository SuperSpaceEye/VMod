package net.spaceeye.vmod.toolgun.modes

import net.spaceeye.vmod.toolgun.modes.state.*

// SHOULD BE INITIALIZED BEFORE OTHER OBJECTS
object ToolgunModes {
    val modes = listOf<BaseMode>(
        ConnectionMode(),
        RopeMode(),
        HydraulicsMode(),
        PhysRopeMode(),
        SliderMode(),
//        WinchMode(),

        ThrusterMode(),
        GravChangerMode(),
        DisableCollisionsMode(),
        SchemMode(),
        ScaleMode(),
        StripMode(),
    )
    var initialized = false
    init {
        initialized = true

        modes.forEach { it.init(BaseNetworking.EnvType.Client) }
    }
}