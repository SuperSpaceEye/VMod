package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.GearMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.COMMON_HUD_5
import net.spaceeye.vmod.translate.GEAR_HUD_1
import net.spaceeye.vmod.translate.get

interface GearHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as GearMode
        when {
            !primaryFirstRaycast -> makeText(COMMON_HUD_5.get())
            primaryFirstRaycast -> makeText(GEAR_HUD_1.get())
        }
    }
}