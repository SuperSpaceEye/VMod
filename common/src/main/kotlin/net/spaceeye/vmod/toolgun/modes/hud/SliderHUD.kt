package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SLIDER_HUD_1
import net.spaceeye.vmod.translate.SLIDER_HUD_2
import net.spaceeye.vmod.translate.SLIDER_HUD_3
import net.spaceeye.vmod.translate.SLIDER_HUD_4
import net.spaceeye.vmod.translate.get

interface SliderHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as SliderMode
        when (primaryTimes) {
            0 -> makeText(SLIDER_HUD_1.get())
            1 -> makeText(SLIDER_HUD_2.get())
            2 -> makeText(SLIDER_HUD_3.get())
            3 -> makeText(SLIDER_HUD_4.get())
        }
    }
}