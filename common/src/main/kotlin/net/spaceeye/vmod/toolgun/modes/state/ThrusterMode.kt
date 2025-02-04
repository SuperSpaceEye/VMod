package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.extensions.SignalActivator
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.types.entities.ThrusterMConstraint
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.ConeBlockRenderer
import net.spaceeye.vmod.toolgun.modes.gui.ThrusterGUI
import net.spaceeye.vmod.toolgun.modes.hud.ThrusterHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePosition
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.getQuatFromDir
import org.valkyrienskies.mod.common.getShipManagingPos

//TODO rework everything
class ThrusterMode: ExtendableToolgunMode(), ThrusterHUD, ThrusterGUI {
    @JsonIgnore private var i = 0

    var force: Double by get(i++, 10000.0, {DoubleLimit(1.0, 1e100).get(it)})
    var channel: String by get(i++, "thruster", {ServerLimits.instance.channelLength.get(it)})
    var scale: Double by get(i++, 1.0, {ServerLimits.instance.thrusterScale.get(it)})


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel

        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

        val pos = getModePosition(posMode, raycastResult, precisePlacementAssistSideNum)
        val basePos = pos + raycastResult.globalNormalDirection!! * 0.5

        level.makeManagedConstraint(ThrusterMConstraint(
            ship.id,
            basePos,
            raycastResult.blockPosition,
            -raycastResult.globalNormalDirection!!,
            force, channel
        ).addExtension(RenderableExtension(ConeBlockRenderer(
            basePos, getQuatFromDir(raycastResult.globalNormalDirection!!), scale.toFloat(), ship.id
        ))).addExtension(SignalActivator(
            "channel", "percentage"
        )).addExtension(Strippable())){it.addFor(player)}
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(ThrusterMode::class) {
                it.addExtension<ThrusterMode> {
                    BasicConnectionExtension<ThrusterMode>("thruster_mode"
                        ,leftFunction = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                    )
                }.addExtension<ThrusterMode> {
                    PlacementModesExtension(false)
                }
            }
        }
    }
}