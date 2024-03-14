package net.spaceeye.vsource.toolgun.modes

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