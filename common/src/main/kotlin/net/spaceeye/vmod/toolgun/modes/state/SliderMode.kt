package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SliderMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.gui.SliderGUI
import net.spaceeye.vmod.toolgun.modes.hud.SliderHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions

class SliderMode: ExtendableToolgunMode(), SliderGUI, SliderHUD {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it) })


    var posMode: PositionModes = PositionModes.NORMAL
    var precisePlacementAssistSideNum: Int = 3

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
                axisRes2.blockPosition, shipRes2.blockPosition).toList()
        ).addExtension(Strippable())){it.addFor(player)}

        sresetState(player)
    }

    fun sresetState(player: ServerPlayer) {
        ServerToolGunState.s2cTooglunWasReset.sendToClient(player, EmptyPacket())
        resetState()
    }

    override fun eResetState() {
        shipRes1 = null
        shipRes2 = null
        axisRes1 = null
        primaryTimes = 0
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(SliderMode::class) {
                it.addExtension<SliderMode> {
                    BasicConnectionExtension<SliderMode>("slider_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryTimes++; inst.refreshHUD() }
                    )
                }.addExtension<SliderMode> {
                    BlockMenuOpeningExtension<SliderMode> { inst -> inst.primaryTimes != 0 }
                }.addExtension<SliderMode> {
                    PlacementModesExtension(true, {mode -> it.posMode = mode}, {num -> it.precisePlacementAssistSideNum = num})
                }
            }
        }
    }
}