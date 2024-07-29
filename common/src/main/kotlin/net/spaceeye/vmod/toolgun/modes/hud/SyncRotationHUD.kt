package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.SyncRotation
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD

interface SyncRotationHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as SyncRotation
        when {
            !primaryFirstRaycast -> makeText("LMB to select first")
            primaryFirstRaycast -> makeText("LMB to select second ship")
        }
    }
}