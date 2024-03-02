package net.spaceeye.vsource.toolgun.modes

object ToolgunModes {
    val modes = listOf<BaseMode>(WeldMode(), RopeMode(), AABBWeldMode(), MuscleMode(), TestMode())
    var initialized = false
    init {
        initialized = true
    }
}