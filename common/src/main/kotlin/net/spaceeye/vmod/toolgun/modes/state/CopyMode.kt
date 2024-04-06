package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.S2CSendTraversalInfo
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.translate.GUIComponents.COPY
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.copyShipWithConnections
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos

object CopyNetworking {
    private var obj: CopyMode? = null
    fun init(mode: CopyMode) {if (obj == null) { obj = mode} }

    // Client has no information about constraints, so server should send it to the client
    val s2cSendTraversalInfo = "send_traversal_info" idWithConns {
        object : S2CConnection<S2CSendTraversalInfo>(it, "copy_networking") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = S2CSendTraversalInfo(buf)

                obj!!.clientTraversalInfo = pkt.data
            }
        }
    }

    infix fun <TT: Serializable> String.idWithConns(constructor: (String) -> S2CConnection<TT>): S2CConnection<TT> {
        val instance = constructor(this)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}

class CopyMode: BaseMode {
    override fun serverSideVerifyLimits() {}
    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}

    override val itemName: Component
        get() = COPY

    override fun makeGUISettings(parentWindow: UIBlock) {}

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }


        return EventResult.interruptFalse()
    }

    init {
        CopyNetworking.init(this)
    }

    var clientTraversalInfo: LongArray? = null



    var serverCaughtShip: ServerShip? = null

    val conn_primary = register { object : C2SConnection<CopyMode>("copy_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<CopyMode>(context.player, buf, ::CopyMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<CopyMode>("copy_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<CopyMode>(context.player, buf, ::CopyMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }


    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {resetState(); return}
        player as ServerPlayer

        serverCaughtShip = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        val traversed = VSConstraintsKeeper.traverseGetConnectedShips(serverCaughtShip!!.id)

        CopyNetworking.s2cSendTraversalInfo.sendToClient(player, S2CSendTraversalInfo(traversed.traversedShipIds))
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (serverCaughtShip == null) {return}
        if (raycastResult.state.isAir) {return}
        val serverCaughtShip = serverCaughtShip!!

        copyShipWithConnections(level, serverCaughtShip, raycastResult)
    }

    override fun resetState() {
        serverCaughtShip = null


        clientTraversalInfo = null
    }
}