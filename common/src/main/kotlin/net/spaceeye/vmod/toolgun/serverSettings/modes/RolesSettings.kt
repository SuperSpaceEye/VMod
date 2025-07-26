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
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.guiElements.*
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.RolePermissionsData
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.translate.*
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

        val entry = makeTextEntry(ROLE_NAME.get(), ::name, 2f, 2f, this, StrLimit(50))
        entry.focus()

        val btns = UIContainer() constrain {
            x = 0.pixels
            y = SiblingConstraint()

            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        Button(Color.GRAY.brighter().brighter(), OK.get()) {
            onOk(name)
        } constrain {
            x = SiblingConstraint(2f)
            y = CenterConstraint()

            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        } childOf btns

        Button(Color.GRAY.brighter().brighter(), CANCEL.get()) {
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
    override val itemName = ROLES_SETTINGS

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = null
        state = null

        dataCallback = { state ->
            Button(Color(180, 180, 180), APPLY_NEW_ROLE_PERMISSIONS.get()) {
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

            Button(Color(180, 180, 180), ENABLE_ALL.get()) {
                currentDisplayed.forEach { if (it is CheckBox) it.setState(true) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            Button(Color(180, 180, 180), DISABLE_ALL.get()) {
                currentDisplayed.forEach { if (it is CheckBox) it.setState(false) }
            } constrain {
                x = SiblingConstraint(2f)
                y = CenterConstraint()
                width = 50.percent
            } childOf enableDisableButtonsHolder

            Button(Color(180, 180, 180), NEW_ROLE.get()) {
                var form: NewRoleForm? = null
                form = NewRoleForm({
                    if (callback != null) {return@NewRoleForm}
                    callback = {
                        parentWindow.clearChildren()
                        makeGUISettings(parentWindow)
                    }
                    c2sTryAddNewRole.sendToServer(C2SNewRole(form!!.name))
                }, {
                    parentWindow.removeChild(form!!)
                })
                form childOf parentWindow
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
                width = 98.percent
            } childOf parentWindow

            DropDown(ROLES.get(), state.rolesPermissions.map { (role, permissions) -> DItem(role, false) {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
                currentDisplayed.clear()

                currentDisplayed.add(Button(Color(180, 0, 0), REMOVE.get()) {
                    if (callback != null) {return@Button}
                    callback = {
                        parentWindow.clearChildren()
                        makeGUISettings(parentWindow)
                    }
                    c2sTryRemoveRole.sendToServer(C2SRemoveRole(role))
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

        val c2sRequestRoleData = regC2S<EmptyPacket>(MOD_ID, "request_role_data", "player_roles_settings") { pkt, player ->
            s2cSendRoleData.sendToClient(player, RolePermissionsData())
        }

        val s2cSendRoleData = regS2C<RolePermissionsData>(MOD_ID, "send_role_data", "player_roles_settings") {pkt ->
            state = pkt
            dataCallback!!(pkt)
            dataCallback = null
        }

        val c2sTryUpdateRolesSettings = regC2S<C2SRolesSettingsUpdate>(MOD_ID, "try_update_roles_settings", "player_roles_settings",
            { pkt, player -> player.hasPermissions(4) }) {pkt, player ->
            synchronized(PlayerAccessManager.rolesPermissions) {
                pkt.rolesPermissions.forEach { (k, v) ->
                    PlayerAccessManager.rolesPermissions[k] = v
                }
            }
        }

        val c2sTryRemoveRole = regC2S<C2SRemoveRole>(MOD_ID, "try_remove_role", "player_roles_settings",
            { pkt, player -> player.hasPermissions(4) }) {(roleName), player ->
            PlayerAccessManager.removeRole(roleName)
            s2cRoleWasRemoved.sendToClient(player, EmptyPacket())
        }

        val s2cRoleWasRemoved = regS2C<EmptyPacket>(MOD_ID, "role_was_removed", "player_roles_settings") {
            callback?.invoke()
            callback = null
        }

        val c2sTryAddNewRole = regC2S<C2SNewRole>(MOD_ID, "try_add_new_role", "player_roles_settings",
            { pkt, player ->  player.hasPermissions(4) }) {(name), player ->
            PlayerAccessManager.addRole(name)
            s2cRoleWasAdded.sendToClient(player, EmptyPacket())
        }

        val s2cRoleWasAdded = regS2C<EmptyPacket>(MOD_ID, "role_was_added", "player_roles_settings") {
            callback?.invoke()
            callback = null
        }

        data class C2SNewRole(var name: String): AutoSerializable

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

        data class C2SRemoveRole(var roleName: String): AutoSerializable
    }
}