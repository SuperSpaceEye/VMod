package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.*
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.RolePermissionsData
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.FakeKProperty
import java.awt.Color

class RolesPermissionsSettings: ServerSettingsGUIBuilder {
    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Roles Permissions Settings")

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = { state ->
            Button(Color(180, 180, 180), "Apply new Role Permissions") {
                c2sTryUpdateRolesSettings.sendToServer(C2SRolesSettingsUpdate(state.rolesPermissions))
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels

                width = 98.percent
            } childOf parentWindow

            val currentDisplayed = mutableListOf<CheckBox>()

            val enableDisableButtonsHolder = UIContainer() constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
                width = 98.percent
                height = ChildBasedMaxSizeConstraint()
            } childOf parentWindow

            Button(Color(180, 180, 180), "Enable All") {
                currentDisplayed.forEach { it.setState(true) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            Button(Color(180, 180, 180), "Disable All") {
                currentDisplayed.forEach { it.setState(false) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            DropDown("Roles", state.rolesPermissions.map { (role, permissions) -> DItem(role, false) {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
                currentDisplayed.clear()

                state.allPermissionsList.forEach { permission ->
                    currentDisplayed.add(makeCheckBox(permission, FakeKProperty({permissions.contains(permission)}, {enable ->
                        if (enable) permissions.add(permission) else permissions.remove(permission)
                    }), 2f, 2f, parentWindow))
                }
            }
            }, onClose = {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
            }, onOpen = {
                currentDisplayed.forEach { it childOf parentWindow }
            }) constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
            } childOf parentWindow
        }
        c2sRequestRoleData.sendToServer(EmptyPacket())
    }

    companion object {
        var state: RolePermissionsData? = null
        var callback: ((RolePermissionsData) -> Unit)? = null

        val c2sRequestRoleData = regC2S<EmptyPacket>("request_role_data", "player_roles_settings") { pkt, player ->
            s2cSendRoleData.sendToClient(player, RolePermissionsData())
        }

        val s2cSendRoleData = regS2C<RolePermissionsData>("send_role_data", "player_roles_settings") {pkt ->
            state = pkt
            callback!!(pkt)
            callback = null
        }

        val c2sTryUpdateRolesSettings = regC2S<C2SRolesSettingsUpdate>("try_update_roles_settings", "player_roles_settings", {
            ServerToolGunState.playerHasAccess(it)
        }) {pkt, player ->
            synchronized(PlayerAccessManager.rolesPermissions) {
                pkt.rolesPermissions.forEach { (k, v) ->
                    PlayerAccessManager.rolesPermissions[k] = v
                }
            }
        }

        class C2SRolesSettingsUpdate(): Serializable {
            var rolesPermissions = mutableMapOf<String, MutableSet<String>>()

            constructor(rolesPermissions: MutableMap<String, MutableSet<String>>): this() {
                this.rolesPermissions = rolesPermissions
            }

            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()

                val schema = state!!.allPermissionsList.mapIndexed { i, item -> Pair(item, i) }.associate { it }
                buf.writeCollection(rolesPermissions.toList()) { buf, it ->
                    buf.writeUtf(it.first)
                    //TODO not optimal but do i care about it?
                    buf.writeCollection(it.second) { buf, it -> buf.writeVarInt(schema[it]!!)}
                }

                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                synchronized(PlayerAccessManager.allPermissionsList) {
                    rolesPermissions =
                        (buf.readCollection({ mutableListOf<Pair<String, MutableSet<String>>>() }) { buf ->
                            Pair(
                                buf.readUtf(),
                                buf.readCollection({ mutableSetOf() }) { buf -> PlayerAccessManager.allPermissionsList[buf.readVarInt()] }
                            )
                        }).associate { it }.toMutableMap()
                }
            }
        }
    }
}