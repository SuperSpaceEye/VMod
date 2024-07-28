package net.spaceeye.vmod.toolgun

import dev.architectury.networking.NetworkManager
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.NetworkingRegisteringFunctions
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.translate.REMOVED
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerToolgunState(var mode: BaseMode)

fun sendHUDErrorToPlayer(player: ServerPlayer, error: String) {
    ServerToolGunState.s2cErrorHappened.sendToClient(player, ServerToolGunState.S2CErrorHappened(error))
}

fun sendHUDErrorToOperators(error: String) {
    ServerLevelHolder.server!!.playerList.players.forEach {
        if (!it.hasPermissions(4)) {return@forEach}
        sendHUDErrorToPlayer(it, error)
    }
}

object ServerToolGunState: ServerClosable(), NetworkingRegisteringFunctions {
    val playersStates = ConcurrentHashMap<UUID, PlayerToolgunState>()
    val playersConstraintsStack = ConcurrentHashMap<UUID, MutableList<ManagedConstraintId>>()

    @JvmStatic fun playerHasAccess(player: ServerPlayer): Boolean {
        return      player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_TOOLGUN_PERMISSION_LEVEL)
                && !ToolgunPermissionManager.getDisallowedPlayers().contains(player.uuid)
                ||  ToolgunPermissionManager.getAllowedPlayers().contains(player.uuid)
    }

    @JvmStatic inline fun <T> verifyPlayerAccessLevel(player: ServerPlayer, fn: () -> T) {
        if (!playerHasAccess(player)) {
            s2cToolgunUsageRejected.sendToClient(player, S2CToolgunUsageRejectedPacket())
            return
        }
        fn()
    }

    override fun close() {
        playersStates.clear()
        playersConstraintsStack.clear()
    }

    class S2CToolgunUsageRejectedPacket(): Serializable {
        override fun serialize(): FriendlyByteBuf {return getBuffer()}
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    val s2cToolgunUsageRejected = "toolgun_usage_rejected" idWithConns {
        object : S2CConnection<S2CToolgunUsageRejectedPacket>(it, "server_toolgun") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
                ClientToolGunState.currentMode?.resetState()
                //TODO
                ClientToolGunState.addHUDError("You don't have the permission to use toolgun")
            } }
        }
    }

    class C2SRequestRemoveLastConstraintPacket(): Serializable {
        override fun serialize(): FriendlyByteBuf {return getBuffer()}
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    val c2sRequestRemoveLastConstraint = "request_remove_last_constraint" idWithConnc {
        object : C2SConnection<C2SRequestRemoveLastConstraintPacket>(it, "server_toolgun") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = verifyPlayerAccessLevel(context.player as ServerPlayer) {
                val stack = playersConstraintsStack[context.player.uuid] ?: return
                var item: ManagedConstraintId = stack.removeLastOrNull() ?: return

                val level = context.player.level() as ServerLevel

                // if constraint wasn't already removed, then remove it
                while (true) {
                    if (level.removeManagedConstraint(item)) {
                        break
                    } else {
                        item = stack.removeLastOrNull() ?: return
                    }
                }

                context.player.sendSystemMessage(REMOVED)
            }
        }
    }

    class S2CErrorHappened(): Serializable {
        var errorStr: String = ""

        constructor(s: String): this() {errorStr = s}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeUtf(errorStr)

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            errorStr = buf.readUtf()
        }
    }

    val s2cErrorHappened = "error_happened" idWithConns {
        object : S2CConnection<S2CErrorHappened>(it, "server_toolgun") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = S2CErrorHappened()
                pkt.deserialize(buf)

                ClientToolGunState.addHUDError(pkt.errorStr)
            }
        }
    }

    class S2CToolgunWasReset(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    val s2cTooglunWasReset = "toolgun_was_reset" idWithConns {
        object : S2CConnection<S2CToolgunWasReset>(it, "server_toolgun") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                ClientToolGunState.currentMode?.resetState()
                ClientToolGunState.currentMode?.refreshHUD()
            }
        }
    }

    class C2SToolgunWasReset(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    val c2sToolgunWasReset = "toolgun_was_reset" idWithConnc {
        object : C2SConnection<C2SToolgunWasReset>(it, "server_toolgun") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val player = context.player ?: return
                playersStates[player.uuid]?.mode?.resetState()
            }
        }
    }
}