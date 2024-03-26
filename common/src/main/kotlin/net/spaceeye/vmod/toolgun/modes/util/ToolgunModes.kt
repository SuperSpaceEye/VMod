package net.spaceeye.vmod.toolgun.modes.util

import net.spaceeye.vmod.toolgun.modes.*
import net.spaceeye.vmod.toolgun.modes.state.*

// SHOULD BE INITIALIZED BEFORE OTHER OBJECTS
object ToolgunModes {
    val modes = listOf<BaseMode>(
        WeldMode(),
        RopeMode(),
        HydraulicsMode(),
        AxisMode(),


        StripMode()
    )
    var initialized = false
    init {
        initialized = true
    }
}