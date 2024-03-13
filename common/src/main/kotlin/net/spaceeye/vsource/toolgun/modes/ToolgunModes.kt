package net.spaceeye.vsource.toolgun.modes

object ToolgunModes {
    val modes = listOf<BaseMode>(
        WeldMode(),
        RopeMode(),
        AABBWeldMode(),
        HydraulicsMode(),
        AxisMode(),


        StripMode()
    )
    var initialized = false
    init {
        initialized = true
    }
}