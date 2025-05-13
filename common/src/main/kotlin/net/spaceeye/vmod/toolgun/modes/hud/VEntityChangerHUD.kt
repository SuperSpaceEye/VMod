package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.VEntityChanger
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.VENTITY_CHANGER_HUD_1
import net.spaceeye.vmod.translate.VENTITY_CHANGER_HUD_2
import net.spaceeye.vmod.translate.get

interface VEntityChangerHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        if (VEntityChanger.clientVEntities.isEmpty()) {
            makeText(VENTITY_CHANGER_HUD_1.get())
        } else {
            makeText(VENTITY_CHANGER_HUD_2.get())
        }
    }
}