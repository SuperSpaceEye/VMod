package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.PlayersRolesData
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.EmptyPacket
import java.awt.Color

class PlayersRolesSettings: ServerSettingsGUIBuilder {
    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Players Roles Settings")

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = {data ->
            data.playersRoles.forEach { (_, item) ->


                UIText("${item.nickname} ${item.role}", false) constrain {
                    x = 2.pixels
                    y = SiblingConstraint(2f) + 2f.pixels

                    color = Color.BLACK.toConstraint()
                } childOf parentWindow
            }
        }
        c2sRequestPlayersRolesData.sendToServer(EmptyPacket())
    }

    companion object {
        var callback: ((PlayersRolesData) -> Unit)? = null
        val c2sRequestPlayersRolesData = regC2S<EmptyPacket>("request_players_roles_data", "players_roles_settings") {pkt, player ->
            s2cSendPlayersRolesData.sendToClient(player, PlayersRolesData())
        }
        val s2cSendPlayersRolesData = regS2C<PlayersRolesData>("send_players_roles_data", "players_roles_settings") {pkt ->
            callback!!(pkt)
            callback = null
        }
    }
}