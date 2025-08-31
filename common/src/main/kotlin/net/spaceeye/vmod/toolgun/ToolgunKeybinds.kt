package net.spaceeye.vmod.toolgun

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping

object ToolgunKeybinds {
    val GUI_MENU_OPEN_OR_CLOSE = register(
        KeyMapping(
            "key.vmod.gui_open_or_close",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_TAB,
            "vmod.keymappings_name"
        ))

    val TOOLGUN_REMOVE_TOP_VENTITY = register(
        KeyMapping(
            "key.vmod.remove_top_ventity",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_RESET_KEY = register(
        KeyMapping(
            "key.vmod.reset_ventity_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_TOGGLE_HUD_KEY = register(
        KeyMapping(
            "key.vmod.toggle_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_TOGGLE_HUD_INFO_KEY = register(
        KeyMapping(
            "key.vmod.toggle_hud_info",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_I,
            "vmod.keymappings_name"
        )
    )

    val TOOLGUN_CHANGE_PRESET_KEY = register(
        KeyMapping(
            "key.vmod.change_preset_key",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            "vmod.keymappings_name"
        )
    )

    private fun register(keyMapping: KeyMapping): KeyMapping {
        KeyMappingRegistry.register(keyMapping)
        return keyMapping
    }
}