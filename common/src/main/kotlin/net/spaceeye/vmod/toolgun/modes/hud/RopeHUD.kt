package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.COMMON_HUD_2
import net.spaceeye.vmod.translate.COMMON_HUD_5
import net.spaceeye.vmod.translate.get

interface RopeHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as RopeMode
        when {
            !primaryFirstRaycast -> makeText(COMMON_HUD_5.get())
            primaryFirstRaycast -> makeText(COMMON_HUD_2.get())
        }
    }
}