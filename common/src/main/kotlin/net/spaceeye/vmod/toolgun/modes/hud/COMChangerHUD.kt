package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.COMChangerMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.COM_CHANGER_HUD_1
import net.spaceeye.vmod.translate.get

interface COMChangerHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as COMChangerMode
        makeText(COM_CHANGER_HUD_1.get())
    }
}