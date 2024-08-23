package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.RolePermissionsData
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.EmptyPacket

class PlayerRoleManager: ServerSettingsGUIBuilder {
    override val itemName: TranslatableComponent
        get() = TranslatableComponent("AAAAAAAAAAAAAAAA")

    override fun makeGUISettings(parentWindow: UIContainer) {
        if (callback != null) {return}
        callback = {
        }
        c2sRequestRoleData.sendToServer(EmptyPacket())
    }

    companion object {
        var callback: ((RolePermissionsData) -> Unit)? = null
        val c2sRequestRoleData = regC2S<EmptyPacket>("request_role_data", "player_role_manager") { pkt, player ->
            s2cSendRoleData.sendToClient(player, RolePermissionsData())
        }
        val s2cSendRoleData = regS2C<RolePermissionsData>("send_role_data", "player_role_manager") {pkt ->
            callback!!(pkt)
            callback = null
        }
    }
}