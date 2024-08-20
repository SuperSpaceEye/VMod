package net.spaceeye.vmod.toolgun.serverSettings

import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.serverSettings.modes.DimensionalGravitySettings
import net.spaceeye.vmod.toolgun.serverSettings.modes.ServerLimitsSettings
import net.spaceeye.vmod.utils.Registry

interface ServerSettingsGUIBuilder: GUIBuilder

object ServerSettingsTypes: Registry<ServerSettingsGUIBuilder>() {
    init {
        register(ServerLimitsSettings::class)
        register(DimensionalGravitySettings::class)
    }
}