package net.spaceeye.vmod.toolgun.clientSettings

import net.spaceeye.vmod.toolgun.clientSettings.modes.ClientLimitsSettings
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.utils.Registry

interface ClientSettingsGUIBuilder: GUIBuilder

object ClientSettingsTypes: Registry<ClientSettingsGUIBuilder>() {
    init {
        register(ClientLimitsSettings::class)
    }
}