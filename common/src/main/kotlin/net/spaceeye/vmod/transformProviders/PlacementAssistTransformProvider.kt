package net.spaceeye.vmod.transformProviders

import net.minecraft.client.Minecraft
import net.spaceeye.vmod.toolgun.modes.PositionModes
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getQuatFromDir
import net.spaceeye.vmod.utils.posShipToWorldRender
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.getShipManagingPos

class PlacementAssistTransformProvider(
    var firstResult: RaycastFunctions.RaycastResult,
    var mode: PositionModes,
    var ship1: ClientShip
): ClientShipTransformProvider {
    val level = Minecraft.getInstance().level!!
    val player = Minecraft.getInstance().cameraEntity!!

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        var secondResult = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            )
        )

        val ship = level.getShipManagingPos(secondResult.blockPosition!!)
        if (ship != null && ship.id == ship1.id || secondResult.state.isAir) {
            secondResult = RaycastFunctions.raycastNoShips(
                level,
                RaycastFunctions.Source(
                    Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                    Vector3d(Minecraft.getInstance().player!!.eyePosition)
                )
            )
        }

        // not sure why i need to flip normal but it works
        val dir1 = when {
            firstResult.globalNormalDirection!!.y ==  1.0 -> -firstResult.globalNormalDirection!!
            firstResult.globalNormalDirection!!.y == -1.0 -> -firstResult.globalNormalDirection!!
            else -> firstResult.globalNormalDirection!!
        }
        val dir2 = secondResult.worldNormalDirection!!

        var rotation = Quaterniond()
        if (!secondResult.state.isAir) {
            // this rotates ship so that it aligns with hit pos normal
            rotation = getQuatFromDir(dir1).normalize()
            // this rotates ship to align with world normal
            rotation = getQuatFromDir(dir2).mul(rotation).normalize()
        }

        val spoint1 = if (mode == PositionModes.NORMAL) {firstResult.globalHitPos!!} else {firstResult.globalCenteredHitPos!!}
        val rpoint2 = if (mode == PositionModes.NORMAL) {secondResult.worldHitPos!!} else {secondResult.worldCenteredHitPos!!}

        // ship transform modifies both position in world AND rotation, but while we don't care about position in world,
        // rotation is incredibly important
        val point = rpoint2 - (
            posShipToWorldRender(ship1, spoint1) - posShipToWorldRender(
                ship1,
                Vector3d(ship1.renderTransform.positionInShip)
            )
        )

        return ShipTransformImpl(
            point.toJomlVector3d(),
            shipTransform.positionInShip,
            rotation,
            shipTransform.shipToWorldScaling
        )
    }

    override fun provideNextTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        latestNetworkTransform: ShipTransform
    ): ShipTransform {
        return shipTransform
    }
}