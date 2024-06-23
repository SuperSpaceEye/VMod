package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.GravChangerMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.GRAV_CHANGER_HUD_1
import net.spaceeye.vmod.translate.get

interface GravChangerHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as GravChangerMode
        makeText(GRAV_CHANGER_HUD_1.get())
    }
}