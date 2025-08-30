package net.spaceeye.vmod.toolgun

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.gui.addScreenAddition
import net.spaceeye.vmod.gui.additions.InfoAddition
import net.spaceeye.vmod.gui.additions.InfoAdditionNetworking
import net.spaceeye.vmod.gui.additions.PresetsAddition
import net.spaceeye.vmod.gui.additions.VEntityChangerWorldMenu
import net.spaceeye.vmod.toolgun.gui.ClientSettingsGUI
import net.spaceeye.vmod.toolgun.gui.ServerSettingsGUI
import net.spaceeye.vmod.toolgun.gui.SettingPresets
import net.spaceeye.vmod.toolgun.gui.ToolgunGUI
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.translate.CLIENT_SETTINGS
import net.spaceeye.vmod.translate.MAIN
import net.spaceeye.vmod.translate.SERVER_SETTINGS
import net.spaceeye.vmod.translate.SETTINGS_PRESETS
import net.spaceeye.vmod.utils.Registry

private var instance: ToolgunInstance? = null
    get() {
        if (field != null) return field
        field = ToolgunInstance(
            MOD_ID,
            ToolgunModes
        )

        field!!.server = ServerToolGunState(field!!)
        field!!.server.instance = field!!

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            field!!.client = ClientToolGunState(field!!)
            field!!.client.instance = field!!
            field!!.client.initState()

            field!!.client.addWindow(MAIN) {ToolgunGUI(it, field!!.client)}
            field!!.client.addWindow(CLIENT_SETTINGS) {ClientSettingsGUI(it)}
            field!!.client.addWindow(SERVER_SETTINGS) {ServerSettingsGUI(it, field!!)}

            //TODO think of a better way
            field!!.instanceStorage.put("Presettable-dir-name", "VMod-Presets")
            field!!.client.addWindow(SETTINGS_PRESETS) {SettingPresets(it, field!!)}

        } }

        addScreenAddition { PresetsAddition()       .also { it.instance = field!! } }
        addScreenAddition { VEntityChangerWorldMenu .also { it.instance = field!! } }

        addScreenAddition(field!!, InfoAdditionNetworking) { InfoAddition().also { it.instance = field!! } }

        return field
    }

val VMToolgun: ToolgunInstance get() = instance!!

class ToolgunInstance(
    var modId: String,
    var modeTypes: Registry<BaseMode>
) {
    lateinit var client: ClientToolGunState
    lateinit var server: ServerToolGunState

    val instanceStorage = mutableMapOf<String, Any>()
}