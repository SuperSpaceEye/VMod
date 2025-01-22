package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.ScaleGUI
import net.spaceeye.vmod.toolgun.modes.hud.ScaleHUD
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld

class ScaleMode: ExtendableToolgunMode(), ScaleGUI, ScaleHUD {
    @JsonIgnore private var i = 0

    var scale: Double by get(i++, 1.0, {ServerLimits.instance.scale.get(it)})
    var scaleAllConnected: Boolean by get(i++, true)

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship: ServerShip = raycastResult.ship as ServerShip? ?: return

        if (scaleAllConnected) {
            teleportShipWithConnected(level, ship, Vector3d(ship.transform.positionInWorld), Quaterniond(ship.transform.shipToWorldRotation), scale)
        } else {
            level.shipObjectWorld.teleportShip(ship, ShipTeleportDataImpl(ship.transform.positionInWorld, ship.transform.shipToWorldRotation, ship.velocity, ship.omega, ship.chunkClaimDimension, scale))
        }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(ScaleMode::class) {
                it.addExtension<ScaleMode> {
                    BasicConnectionExtension<ScaleMode>("scale_mode"
                        ,primaryFunction = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}