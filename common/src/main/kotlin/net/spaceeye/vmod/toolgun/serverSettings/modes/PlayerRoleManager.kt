package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.PlayerAccessState
import net.spaceeye.vmod.toolgun.PlayersRolesData
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.translate.OFFLINE_PLAYERS
import net.spaceeye.vmod.translate.ONLINE_PLAYERS
import net.spaceeye.vmod.translate.PLAYER_ROLE_MANAGER
import net.spaceeye.vmod.translate.ROLES
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.EmptyPacket
import java.awt.Color
import java.util.UUID

class PlayerRoleManager: ServerSettingsGUIBuilder {
    override val itemName = PLAYER_ROLE_MANAGER

    private fun displayPlayerState(item: PlayerAccessState, data: PlayersRolesData, parentWindow: UIContainer) {
        val ctn = UIContainer() constrain {
            x = 2.pixels
            y = SiblingConstraint(2f)

            width = 100.percent - 2.pixels
            height = ChildBasedMaxSizeConstraint()
        }

        val text = UIText("${item.nickname} ${item.role}", false) constrain {
            x = 2.pixels
            y = CenterConstraint()

            color = Color.BLACK.toConstraint()
        } childOf ctn

        val change = makeDropDown(ROLES.get(), ctn, 2f, 2f, data.allRoles.map {
            DItem(it, it == item.role) {
                callback = { data ->
                    text.setText("${item.nickname} ${data.playersRoles[item.uuid]?.role}")
                }

                c2sChangePlayerRole.sendToServer(C2SChangePlayerRole(it, item.uuid))
            }
        }) constrain {
            width = FillConstraint()
            x = SiblingConstraint(2f)
            y = CenterConstraint()
        }

        ctn childOf parentWindow
    }

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = { data ->
            val online = data.online.toSet()

            val onlineData  = mutableListOf<Pair<UUID, PlayerAccessState>>()
            val offlineData = mutableListOf<Pair<UUID, PlayerAccessState>>()

            data.playersRoles.forEach {
                if (online.contains(it.key)) { onlineData .add(it.toPair())
                } else                       { offlineData.add(it.toPair()) }
            }

            makeText(ONLINE_PLAYERS.get(), Color.GREEN, 2f, 2f, parentWindow)
            onlineData .forEach { displayPlayerState(it.second, data, parentWindow) }
            makeText(OFFLINE_PLAYERS.get(), Color.RED, 2f, 2f, parentWindow)
            offlineData.forEach { displayPlayerState(it.second, data, parentWindow) }
        }
        c2sRequestPlayersRolesData.sendToServer(EmptyPacket())
    }

    companion object {
        var callback: ((PlayersRolesData) -> Unit)? = null
        val c2sRequestPlayersRolesData = regC2S<EmptyPacket>(MOD_ID, "request_players_roles_data", "player_role_manager") {pkt, player ->
            s2cSendPlayersRolesData.sendToClient(player, PlayersRolesData())
        }
        val s2cSendPlayersRolesData = regS2C<PlayersRolesData>(MOD_ID, "send_players_roles_data", "player_role_manager") {pkt ->
            callback!!(pkt)
            callback = null
        }

        val c2sChangePlayerRole = regC2S<C2SChangePlayerRole>(MOD_ID, "try_change_player_role", "player_role_manager",
            { pkt, player -> player.hasPermissions(4)}) {pkt, player ->
            PlayerAccessManager.setPlayerRole(pkt.uuid, pkt.newRole)
            s2cSendPlayersRolesData.sendToClient(player, PlayersRolesData())
        }

        data class C2SChangePlayerRole(var newRole: String, var uuid: UUID): AutoSerializable
    }
}