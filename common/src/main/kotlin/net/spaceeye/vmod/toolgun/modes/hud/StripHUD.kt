package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.StripMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.STRIP_HUD_1
import net.spaceeye.vmod.translate.get

interface StripHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as StripMode
        makeText(STRIP_HUD_1.get())
    }
}