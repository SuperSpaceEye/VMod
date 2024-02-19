package net.spaceeye.vsource.toolgun

import net.spaceeye.vsource.toolgun.modes.*

object ToolgunModes {
    val modes = listOf<BaseMode>(WeldMode(), RopeMode(), AABBWeldMode())
    var initialized = false
    init {
        initialized = true
    }
}