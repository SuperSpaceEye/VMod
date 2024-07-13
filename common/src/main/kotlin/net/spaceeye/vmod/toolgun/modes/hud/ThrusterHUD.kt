package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD

interface ThrusterHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
//        this as ThrusterMode
//        makeText(STRIP_HUD_1.get())
    }
}