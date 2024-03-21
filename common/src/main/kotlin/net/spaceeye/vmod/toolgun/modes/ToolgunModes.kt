package net.spaceeye.vmod.toolgun.modes

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