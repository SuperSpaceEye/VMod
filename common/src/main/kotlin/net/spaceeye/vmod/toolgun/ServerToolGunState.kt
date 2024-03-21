package net.spaceeye.vmod.toolgun

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.translate.GUIComponents.REMOVED
import net.spaceeye.vmod.utils.ServerClosable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerToolgunState(var mode: BaseMode)

object ServerToolGunState: ServerClosable() {
    val playersStates = ConcurrentHashMap<UUID, PlayerToolgunState>()
    val playersConstraintsStack = ConcurrentHashMap<UUID, MutableList<ManagedConstraintId>>()

    override fun close() {
        playersStates.clear()
        playersConstraintsStack.clear()
    }

    class C2SRequestRemoveLastConstraintPacket(): Serializable {
        override fun serialize(): FriendlyByteBuf {return getBuffer()}
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    val c2sRequestRemoveLastConstraint = "request_remove_last_constraint" idWithConnc {
        object : C2SConnection<C2SRequestRemoveLastConstraintPacket>(it, "toolgun") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val stack = playersConstraintsStack[context.player.uuid] ?: return
                var item: ManagedConstraintId = stack.removeLastOrNull() ?: return

                val level = context.player.level as ServerLevel

                // if constraint wasn't already removed, then remove it
                while (true) {
                    if (level.removeManagedConstraint(item)) {
                        break
                    } else {
                        item = stack.removeLastOrNull() ?: return
                    }
                }

                context.player.sendMessage(REMOVED, context.player.uuid)
            }
        }
    };

    private infix fun <TT: Serializable> String.idWithConnc(constructor: (String) -> C2SConnection<TT>): C2SConnection<TT> {
        val instance = constructor(this)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}