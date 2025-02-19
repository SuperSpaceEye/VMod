package net.spaceeye.vmod.toolgun

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.config.ExternalDataUtil
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.utils.getMapper
import java.util.UUID

data class PlayerAccessState(val uuid: UUID, val nickname: String, var role: String)

class RolePermissionsData(): Serializable {
    var allRoles = mutableListOf<String>()
    var allPermissionsList = mutableListOf<String>()
    var rolesPermissions = mutableMapOf<String, MutableSet<String>>()

    override fun serialize(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer(512))

        synchronized(PlayerAccessManager) {
            buf.writeCollection(PlayerAccessManager.allRoles) { buf, it -> buf.writeUtf(it) }
            buf.writeCollection(PlayerAccessManager.allPermissionsList) { buf, it -> buf.writeUtf(it) }
            val schema = PlayerAccessManager.allPermissionsList.mapIndexed { i, item -> Pair(item, i) }.associate { it }

            buf.writeCollection(PlayerAccessManager.rolesPermissions.toList()) { buf, it ->
                buf.writeUtf(it.first)
                buf.writeCollection(it.second) { buf, it -> buf.writeVarInt(schema[it]!!) }
            }
        }
        return FriendlyByteBuf(Unpooled.wrappedBuffer(buf.accessByteBufWithCorrectSize()))
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        allRoles = buf.readCollection({ mutableListOf() }) {buf -> buf.readUtf()}
        allPermissionsList = buf.readCollection({ mutableListOf() }) {buf.readUtf()}
        rolesPermissions = (buf.readCollection({mutableListOf<Pair<String, MutableSet<String>>>()}) {
                buf ->Pair(
            buf.readUtf(),
            buf.readCollection({ mutableSetOf() }) {buf -> allPermissionsList[buf.readVarInt()]}
        )
        }).associate { it }.toMutableMap()
    }
}

class PlayersRolesData(): Serializable {
    var playersRoles = mutableMapOf<UUID, PlayerAccessState>()
    var allRoles = mutableListOf<String>()

    override fun serialize(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer(512))

        synchronized(PlayerAccessManager) {
            buf.writeCollection(PlayerAccessManager.playersRoles.toList()) { buf, it -> buf.writeUUID(it.first); buf.writeUtf(it.second.nickname); buf.writeUtf(it.second.role)}
            buf.writeCollection(PlayerAccessManager.allRoles) { buf, it -> buf.writeUtf(it) }
        }

        return FriendlyByteBuf(Unpooled.wrappedBuffer(buf.accessByteBufWithCorrectSize()))
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        playersRoles = buf.readCollection({ mutableListOf<Pair<UUID, PlayerAccessState>>()}) { buf ->
            val uuid = buf.readUUID()
            Pair(uuid, PlayerAccessState(uuid, buf.readUtf(), buf.readUtf()))
        }.associate { it }.toMutableMap()
        allRoles = buf.readCollection({ mutableListOf() }) {buf -> buf.readUtf()}
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PlayerAccessMangerState {
    var rolesPermissions = mutableMapOf<String, MutableSet<String>>()
    var playersRoles = mutableMapOf<UUID, PlayerAccessState>()
    var allRoles = mutableListOf<String>()
}

object PlayerAccessManager {
    var state = PlayerAccessMangerState()

    var allPermissionsList = mutableListOf<String>()
    var rolesPermissions get() = state.rolesPermissions; set(value) {state.rolesPermissions = value}
    var playersRoles get() = state.playersRoles; set(value) {state.playersRoles = value}
    var allRoles get() = state.allRoles; set(value) {state.allRoles = value}

    val allPermissions = mutableSetOf<String>()
    const val defaultRoleName = "default"

    init {
        PlayerEvent.PLAYER_JOIN.register { getPlayerState(it) }

        LifecycleEvent.SERVER_STOPPING.register {
            save()
        }

        LifecycleEvent.SERVER_STARTING.register {
            load()
        }
    }


    @Synchronized fun afterInit() {
        addRole(defaultRoleName)
    }

    @Synchronized private fun getPlayerState(player: ServerPlayer): PlayerAccessState =
        playersRoles.getOrPut(player.uuid) {
            PlayerAccessState(player.uuid, player.gameProfile.name, defaultRoleName)
        }

    @Synchronized fun hasPermission(player: ServerPlayer, permission: String): Boolean {
        if (player.hasPermissions(4)) {return true}
        val state = getPlayerState(player)
        val permissions = rolesPermissions[state.role] ?: return false
        return permissions.contains(permission)
    }

    @Synchronized fun setPlayerRole(player: ServerPlayer, role: String): Boolean {
        if (!rolesPermissions.containsKey(role)) {return false}
        val state = getPlayerState(player)
        state.role = role
        return true
    }

    @Synchronized fun setPlayerRole(player: UUID, role: String): Boolean {
        val state = playersRoles[player] ?: return false
        if (!rolesPermissions.containsKey(role)) {return false}
        state.role = role
        return true
    }

    @Synchronized fun addRole(role: String) {
        if (rolesPermissions.containsKey(role)) { return }
        val permissions = mutableSetOf<String>();
        permissions.addAll(allPermissions)
        rolesPermissions[role] = permissions
        allRoles.add(role)
    }

    @Synchronized fun setRole(role: String, permissions: MutableSet<String>) {
        rolesPermissions[role] = permissions
    }

    @Synchronized fun removeRole(role: String): Boolean {
        if (role == defaultRoleName) {return false}
        return rolesPermissions.remove(role) != null
    }

    @Synchronized fun addPermission(permission: String) {
        if (allPermissions.contains(permission)) { throw AssertionError("Permission already exists") }
        allPermissions.add(permission)
        allPermissionsList.add(permission)
    }

    @Synchronized fun addPermissionToRole(role: String, permission: String) {
        if (!allPermissions.contains(permission)) { throw AssertionError("Permission wasn't registered") }
        rolesPermissions[role]?.add(permission) ?: throw AssertionError("Role doesn't exist")
    }

    @Synchronized fun removePermissionFromRole(role: String, permission: String) {
        if (!allPermissions.contains(permission)) { throw AssertionError("Permission wasn't registered") }
        rolesPermissions[role]?.remove(permission) ?: throw AssertionError("Role doesn't exist")
    }

    @Synchronized fun save() {
        val mapper = getMapper()
        val data = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.state)
        ExternalDataUtil.writeObject("role_data.json", data)
    }

    @Synchronized fun load() {
        val mapper = getMapper()
        try {
            val data = ExternalDataUtil.readObject("role_data.json") ?: return ELOG("Failed to load role data as role_data.json doesn't exist!")
            val obj = mapper.readValue(data, PlayerAccessMangerState::class.java)

            val allRolesSet = allRoles.toSet()
            allRoles.addAll(obj.allRoles.filter { !allRolesSet.contains(it) })

            rolesPermissions = obj.rolesPermissions
            playersRoles = obj.playersRoles

        } catch (e: Exception) {
            ELOG("Failed to load role data because of exception:\n${e.stackTraceToString()}")
        } catch (e: Error) {
            ELOG("Failed to load role data because of error:\n${e.stackTraceToString()}")
        }
    }
}