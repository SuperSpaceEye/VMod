package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD

interface SliderHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as SliderMode
        when (primaryTimes) {
            0 -> makeText("LMB to make first ship point")
            1 -> makeText("LMB to make second ship point (should be the same ship)")
            2 -> makeText("LMB to make first axis point (should be a different ship or a ground)")
            3 -> makeText("LMB to make second axis point (should be the same ship or a ground)")
        }
    }
}