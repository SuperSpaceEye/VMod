package net.spaceeye.vsource.toolgun

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.constraintsManaging.removeManagedConstraint
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.networking.Serializable
import net.spaceeye.vsource.toolgun.modes.BaseMode
import net.spaceeye.vsource.translate.GUIComponents.REMOVED
import net.spaceeye.vsource.utils.ServerClosable
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