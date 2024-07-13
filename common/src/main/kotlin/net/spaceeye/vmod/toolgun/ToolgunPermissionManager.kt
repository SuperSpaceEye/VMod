package net.spaceeye.vmod.toolgun

import net.spaceeye.vmod.VMConfig
import java.util.*

object ToolgunPermissionManager {
    private var allowedPlayersState = mutableSetOf<UUID>()
    private var disallowedPlayersState = mutableSetOf<UUID>()

    init {
        loadState()
    }

    //TODO this is dumb
    private fun saveState() {
        VMConfig.SERVER.PERMISSIONS.ALWAYS_ALLOWED    = allowedPlayersState   .joinToString(",") { it.toString() }
        VMConfig.SERVER.PERMISSIONS.ALWAYS_DISALLOWED = disallowedPlayersState.joinToString(",") { it.toString() }
    }
    private fun loadState() {
        allowedPlayersState    = VMConfig.SERVER.PERMISSIONS.ALWAYS_ALLOWED   .split(",").mapNotNull { try { UUID.fromString(it) } catch (e: Exception) { null } }.toMutableSet()
        disallowedPlayersState = VMConfig.SERVER.PERMISSIONS.ALWAYS_DISALLOWED.split(",").mapNotNull { try { UUID.fromString(it) } catch (e: Exception) { null } }.toMutableSet()
    }

    fun getAllowedPlayers(): Set<UUID> = allowedPlayersState
    fun allowedPlayersRemove(uuid: UUID) { allowedPlayersState.remove(uuid); saveState() }
    fun allowedPlayersAdd(uuid: UUID) { allowedPlayersState.add(uuid); disallowedPlayersState.remove(uuid); saveState() }

    fun getDisallowedPlayers(): Set<UUID> = disallowedPlayersState
    fun disallowedPlayersRemove(uuid: UUID) { disallowedPlayersState.remove(uuid); saveState() }
    fun disallowedPlayersAdd(uuid: UUID) { disallowedPlayersState.add(uuid); allowedPlayersState.remove(uuid); saveState() }
}