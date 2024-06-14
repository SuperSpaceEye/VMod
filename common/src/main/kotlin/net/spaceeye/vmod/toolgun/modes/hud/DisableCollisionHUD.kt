package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.DisableCollisionsMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.COMMON_HUD_2
import net.spaceeye.vmod.translate.DISABLE_COLLISIONS_HUD_1
import net.spaceeye.vmod.translate.get

interface DisableCollisionHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as DisableCollisionsMode
        when {
            !primaryFirstRaycast -> makeText(DISABLE_COLLISIONS_HUD_1.get())
            primaryFirstRaycast -> makeText(COMMON_HUD_2.get())
        }
    }
}