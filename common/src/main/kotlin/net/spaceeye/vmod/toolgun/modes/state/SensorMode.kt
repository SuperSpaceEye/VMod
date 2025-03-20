package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.types.entities.SensorVEntity
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.ConeBlockRenderer
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePosition
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.toolgun.modes.gui.SensorGUI
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.getQuatFromDir
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color

//TODO finish this mf
class SensorMode: ExtendableToolgunMode(), SensorGUI {
    @JsonIgnore private var i = 0

    var maxDistance: Double by get(i++, 10.0) { ServerLimits.instance.maxDistance.get(it) }
    var channel: String by get(i++, "sensor") { ServerLimits.instance.channelLength.get(it) }
    var scale: Double by get(i++, 1.0) { ServerLimits.instance.thrusterScale.get(it) }
    var fullbright: Boolean by get(i++, false)
    var ignoreSelf: Boolean by get(i++, false)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel

        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

        val pos = getModePosition(posMode, raycastResult, precisePlacementAssistSideNum)
        val basePos = pos + raycastResult.globalNormalDirection!! * 0.5

        level.makeVEntity(SensorVEntity(
            ship.id,
            basePos,
            raycastResult.blockPosition,
            raycastResult.globalNormalDirection!!,
            maxDistance, ignoreSelf, scale, channel
        ).addExtension(RenderableExtension(ConeBlockRenderer(
            basePos, getQuatFromDir(raycastResult.globalNormalDirection!!), scale.toFloat(), ship.id, Color(0, 255, 0), fullbright
        ))).addExtension(Strippable())){it.addFor(player)}
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(SensorMode::class) {
                it.addExtension<SensorMode> {
                    BasicConnectionExtension<SensorMode>("sensor_mode"
                        ,leftFunction = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                    )
                }.addExtension<SensorMode> {
                    PlacementModesExtension(false)
                }
            }
        }
    }
}