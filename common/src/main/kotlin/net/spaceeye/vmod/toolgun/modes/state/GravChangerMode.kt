package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.toolgun.modes.gui.GravChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.GravChangerHUD
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class GravChangerMode: ExtendableToolgunMode(), GravChangerHUD, GravChangerGUI {
    enum class Mode {
        Individual,
        AllConnected,
        AllConnectedAndTouching
    }
    @JsonIgnore private var i = 0

    var gravityVector: Vector3d by get(i++, Vector3d(0, -10, 0)).presettable()
    var mode: Mode by get(i++, Mode.Individual).presettable()

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return

        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).also {
                    it.gravityVector = Vector3d(gravityVector)
                    it.useDimensionGravity = false
                }
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach).also {
                    it.gravityVector = Vector3d(gravityVector)
                    it.useDimensionGravity = false
                }
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return


        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).reset()
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach)
                .reset()
        }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(GravChangerMode::class) {
                it.addExtension {
                    BasicConnectionExtension<GravChangerMode>("grav_changer_mode"
                        ,allowResetting = true
                        ,leftFunction  = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction = { item, level, player, rr -> item.activateSecondaryFunction(level, player, rr)}
                    )
                }.addExtension { Presettable() }
            }
        }
    }
}