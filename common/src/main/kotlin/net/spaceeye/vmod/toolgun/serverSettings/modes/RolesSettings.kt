package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.spaceeye.vmod.guiElements.*
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.RolePermissionsData
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.FakeKProperty
import java.awt.Color

class NewRoleForm(
    val onOk: (name: String) -> Unit,
    val onCancel: () -> Unit,
): UIBlock(Color.GRAY.brighter()) {
    var name = ""

    init {
        constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = 150.pixels
            height = 50.pixels
        }

        val entry = makeTextEntry("Role Name", ::name, 2f, 2f, this, StrLimit(50))
        entry.focus()

        val btns = UIContainer() constrain {
            x = 0.pixels
            y = SiblingConstraint()

            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        Button(Color.GRAY.brighter().brighter(), "Ok") {
            onOk(name)
        } constrain {
            x = SiblingConstraint(2f)
            y = CenterConstraint()

            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf btns

        Button(Color.GRAY.brighter().brighter(), "Cancel") {
            onCancel()
        } constrain {
            x = SiblingConstraint(2f)
            y = CenterConstraint()

            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf btns

        btns childOf this
    }
}

class RolesPermissionsSettings: ServerSettingsGUIBuilder {
    override val itemName get() = Component.literal("Roles Settings")

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = null
        state = null

        dataCallback = { state ->
            Button(Color(180, 180, 180), "Apply new Role Permissions") {
                c2sTryUpdateRolesSettings.sendToServer(C2SRolesSettingsUpdate(state.rolesPermissions))
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels

                width = 98.percent
            } childOf parentWindow

            val currentDisplayed = mutableListOf<UIComponent>()

            val enableDisableButtonsHolder = UIContainer() constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
                width = 98.percent
                height = ChildBasedMaxSizeConstraint()
            } childOf parentWindow

            Button(Color(180, 180, 180), "Enable All") {
                currentDisplayed.forEach { if (it is CheckBox) it.setState(true) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            Button(Color(180, 180, 180), "Disable All") {
                currentDisplayed.forEach { if (it is CheckBox) it.setState(false) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            Button(Color(180, 180, 180), "New Role") {
                var form: NewRoleForm? = null
                form = NewRoleForm({
                    if (callback != null) {return@NewRoleForm}
                    callback = {
                        parentWindow.clearChildren()
                        makeGUISettings(parentWindow)
                    }
                    val pkt = C2SNewRole()
                    pkt.name = form!!.name
                    c2sTryAddNewRole.sendToServer(pkt)
                }, {
                    parentWindow.removeChild(form!!)
                })
                form childOf parentWindow
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
                width = 98.percent
            } childOf parentWindow

            DropDown("Roles", state.rolesPermissions.map { (role, permissions) -> DItem(role, false) {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
                currentDisplayed.clear()

                currentDisplayed.add(Button(Color(180, 0, 0), "Remove") {
                    if (callback != null) {return@Button}
                    callback = {
                        parentWindow.clearChildren()
                        makeGUISettings(parentWindow)
                    }
                    val pkt = C2SRemoveRole()
                    pkt.roleName = role
                    c2sTryRemoveRole.sendToServer(pkt)
                } constrain {
                    x = 2f.pixels
                    y = SiblingConstraint() + 2f.pixels
                    width = 98.percent
                } childOf parentWindow )

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
        var dataCallback: ((RolePermissionsData) -> Unit)? = null
        var callback: (() -> Unit)? = null

        val c2sRequestRoleData = regC2S<EmptyPacket>("request_role_data", "player_roles_settings") { pkt, player ->
            s2cSendRoleData.sendToClient(player, RolePermissionsData())
        }

        val s2cSendRoleData = regS2C<RolePermissionsData>("send_role_data", "player_roles_settings") {pkt ->
            state = pkt
            dataCallback!!(pkt)
            dataCallback = null
        }

        val c2sTryUpdateRolesSettings = regC2S<C2SRolesSettingsUpdate>("try_update_roles_settings", "player_roles_settings", {
            it.hasPermissions(4)
        }) {pkt, player ->
            synchronized(PlayerAccessManager.rolesPermissions) {
                pkt.rolesPermissions.forEach { (k, v) ->
                    PlayerAccessManager.rolesPermissions[k] = v
                }
            }
        }

        val c2sTryRemoveRole = regC2S<C2SRemoveRole>("try_remove_role", "player_roles_settings", {
            it.hasPermissions(4)
        }) {pkt, player ->
            PlayerAccessManager.removeRole(pkt.roleName)
            s2cRoleWasRemoved.sendToClient(player, EmptyPacket())
        }

        val s2cRoleWasRemoved = regS2C<EmptyPacket>("role_was_removed", "player_roles_settings") {
            callback?.invoke()
            callback = null
        }

        val c2sTryAddNewRole = regC2S<C2SNewRole>("try_add_new_role", "player_roles_settings",
            {player -> player.hasPermissions(4)}) {pkt, player ->
            PlayerAccessManager.addRole(pkt.name)
            s2cRoleWasAdded.sendToClient(player, EmptyPacket())
        }

        val s2cRoleWasAdded = regS2C<EmptyPacket>("role_was_added", "player_roles_settings") {
            callback?.invoke()
            callback = null
        }

        class C2SNewRole(): Serializable {
            var name: String = ""

            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()
                buf.writeUtf(name)
                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                name = buf.readUtf()
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

        class C2SRemoveRole(): Serializable {
            var roleName: String = ""

            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()

                buf.writeUtf(roleName)

                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                roleName = buf.readUtf()
            }
        }
    }
}