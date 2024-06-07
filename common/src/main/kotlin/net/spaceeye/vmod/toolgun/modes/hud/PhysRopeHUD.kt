package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.PhysRopeMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.*

interface PhysRopeHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as PhysRopeMode
        when {
            !primaryFirstRaycast -> makeText(COMMON_HUD_5.get())
            primaryFirstRaycast -> makeText(COMMON_HUD_2.get())
        }
    }
}