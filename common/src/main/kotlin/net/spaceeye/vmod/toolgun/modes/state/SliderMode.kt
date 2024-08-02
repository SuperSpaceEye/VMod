package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SliderMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.eventsHandling.SliderCEH
import net.spaceeye.vmod.toolgun.modes.gui.SliderGUI
import net.spaceeye.vmod.toolgun.modes.hud.SliderHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions

class SliderMode: BaseMode, SliderCEH, SliderGUI, SliderHUD {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it as Double) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it as Double) })

    override var posMode: PositionModes by get(2, PositionModes.NORMAL)
    override var precisePlacementAssistSideNum: Int by get(3, 3, {ServerLimits.instance.precisePlacementAssistSides.get(it as Int)})

    val conn_primary = register { object : C2SConnection<SliderMode>("slider_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SliderMode>(context.player, buf, ::SliderMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    override var precisePlacementAssistRendererId: Int = -1

    var shipRes1: RaycastFunctions.RaycastResult? = null
    var shipRes2: RaycastFunctions.RaycastResult? = null
    var axisRes1: RaycastFunctions.RaycastResult? = null

    var primaryTimes = 0

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycastAndActivateFn(posMode, precisePlacementAssistSideNum, level, raycastResult) {
        level, shipId, ship, spoint, rpoint, rresult ->

        player as ServerPlayer

        if (shipRes1 == null && rresult.ship == null) { return@serverRaycastAndActivateFn sresetState(player) }
        val shipRes1 = shipRes1 ?: run {
            shipRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val shipRes2 = shipRes2 ?: run {
            shipRes2 = rresult
            if (shipRes1.shipId != shipRes2!!.shipId) { return@serverRaycastAndActivateFn sresetState(player) }
            return@serverRaycastAndActivateFn
        }

        val axisRes1 = axisRes1 ?: run {
            axisRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val axisRes2 = rresult

        if (axisRes1.shipId != axisRes2.shipId) { return@serverRaycastAndActivateFn sresetState(player) }

        val axisPair = getModePositions(posMode, axisRes1, axisRes2, precisePlacementAssistSideNum)
        val shipPair = getModePositions(posMode, shipRes1, shipRes2, precisePlacementAssistSideNum)

        level.makeManagedConstraint(SliderMConstraint(
            axisRes1.shipId, shipRes1.shipId,
            axisPair.first, axisPair.second, shipPair.first, shipPair.second,
            compliance, maxForce, setOf(
                axisRes1.blockPosition, shipRes1.blockPosition,
                axisRes2.blockPosition, shipRes2.blockPosition).toList(), null
        )){it.addFor(player)}

        sresetState(player)
    }

    fun sresetState(player: ServerPlayer) {
        ServerToolGunState.s2cTooglunWasReset.sendToClient(player, EmptyPacket())
        resetState()
    }

    override fun resetState() {
        shipRes1 = null
        shipRes2 = null
        axisRes1 = null
        primaryTimes = 0
    }
}