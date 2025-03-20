package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.MassChangerMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.MASS_CHANGER_HUD_1
import net.spaceeye.vmod.translate.get

interface MassChangerHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as MassChangerMode
        makeText(MASS_CHANGER_HUD_1.get())
    }
}