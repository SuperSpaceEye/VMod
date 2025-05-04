package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.THRUSTER_HUD_1
import net.spaceeye.vmod.translate.get

interface ThrusterHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as ThrusterMode
        makeText(THRUSTER_HUD_1.get())
    }
}