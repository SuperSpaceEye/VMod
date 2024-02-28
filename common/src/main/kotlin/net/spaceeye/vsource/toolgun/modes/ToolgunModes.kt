package net.spaceeye.vsource.toolgun.modes

object ToolgunModes {
    val modes = listOf<BaseMode>(WeldMode(), RopeMode(), AABBWeldMode(), TestMode())
    var initialized = false
    init {
        initialized = true
    }
}