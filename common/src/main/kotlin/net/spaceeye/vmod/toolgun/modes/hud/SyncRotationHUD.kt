package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.SyncRotation
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SYNC_ROTATION_HUD_1
import net.spaceeye.vmod.translate.SYNC_ROTATION_HUD_2
import net.spaceeye.vmod.translate.get

interface SyncRotationHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as SyncRotation
        when {
            !primaryFirstRaycast -> makeText(SYNC_ROTATION_HUD_1.get())
             primaryFirstRaycast -> makeText(SYNC_ROTATION_HUD_2.get())
        }
    }
}