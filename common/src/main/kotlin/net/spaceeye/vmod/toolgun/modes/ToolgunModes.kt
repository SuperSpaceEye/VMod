package net.spaceeye.vmod.toolgun.modes

import net.spaceeye.vmod.toolgun.modes.state.*

// SHOULD BE INITIALIZED BEFORE OTHER OBJECTS
object ToolgunModes {
    val modes = listOf(
        WeldMode(),
        RopeMode(),
        HydraulicsMode(),
        AxisMode(),
        PhysRopeMode(),


        CopyMode(),
        ScaleMode(),
        StripMode()
    )
    var initialized = false
    init {
        initialized = true
    }
}