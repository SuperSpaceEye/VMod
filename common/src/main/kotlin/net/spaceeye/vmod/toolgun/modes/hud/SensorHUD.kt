package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.state.SensorMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SENSOR_HUD_1
import net.spaceeye.vmod.translate.get

interface SensorHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as SensorMode
        makeText(SENSOR_HUD_1.get())
    }
}