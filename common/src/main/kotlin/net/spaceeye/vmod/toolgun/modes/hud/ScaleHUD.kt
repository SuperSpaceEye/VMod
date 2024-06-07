package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SCALE_HUD_1
import net.spaceeye.vmod.translate.get

interface ScaleHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as ScaleMode
        makeText(SCALE_HUD_1.get())
    }
}