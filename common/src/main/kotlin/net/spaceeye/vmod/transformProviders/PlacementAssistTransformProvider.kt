package net.spaceeye.vmod.transformProviders

import net.minecraft.client.Minecraft
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePosition
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.posWorldToShipRender
import net.spaceeye.vmod.utils.vs.transformDirectionShipToWorldRender
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShipRender
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl

class PlacementAssistTransformProvider(
    var firstResult: RaycastFunctions.RaycastResult,
    var mode: PositionModes,
    var ship1: ClientShip,
    var precisePlacementAssistSideNum: Int
): ClientShipTransformProvider {
    val level = Minecraft.getInstance().level!!
    val player = Minecraft.getInstance().cameraEntity!!

    lateinit var rresult2: RaycastFunctions.RaycastResult
    lateinit var spoint1: Vector3d
    lateinit var spoint2: Vector3d

    lateinit var gdir1: Vector3d
    lateinit var gdir2: Vector3d

    val raycastDistance = VMConfig.CLIENT.TOOLGUN.MAX_RAYCAST_DISTANCE

    val ignoreShipIds = mutableSetOf(ship1.id)

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform? {
        //TODO think of a better way
        if (!ToolgunItem.playerIsUsingToolgun()) {return null}
        val secondResult = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            ),
            raycastDistance,
            ignoreShipIds,
            {ship, dir -> transformDirectionShipToWorldRender(ship as ClientShip, dir) },
            {ship, dir -> transformDirectionWorldToShipRender(ship as ClientShip, dir) },
            {ship, pos, transform -> posShipToWorldRender(ship as ClientShip, pos, transform) },
            {ship, pos, transform -> posWorldToShipRender(ship as ClientShip, pos, transform) }
        )
        rresult2 = secondResult

        if (firstResult.globalNormalDirection == null || secondResult.worldNormalDirection == null) { return null }
        // not sure why i need to flip normal but it works
        val dir1 = when {
            firstResult.globalNormalDirection!!.y ==  1.0 -> -firstResult.globalNormalDirection!!
            firstResult.globalNormalDirection!!.y == -1.0 -> -firstResult.globalNormalDirection!!
            else -> firstResult.globalNormalDirection!!
        }
        val dir2 = secondResult.worldNormalDirection!!

        gdir1 = dir1
        gdir2 = secondResult.globalNormalDirection!!

        var rotation = Quaterniond()
        if (!secondResult.state.isAir) {
            rotation = Quaterniond()
                .mul(getQuatFromDir(dir2)) // this rotates ship to align with world normal
                .mul(getQuatFromDir(dir1)) // this rotates ship so that it aligns with hit pos normal
                .normalize()
        }

        spoint1 = getModePosition(mode, firstResult, precisePlacementAssistSideNum)
        spoint2 = getModePosition(mode, secondResult, precisePlacementAssistSideNum)
        val rpoint2 = secondResult.ship?.let {posShipToWorldRender(secondResult.ship as ClientShip, spoint2)} ?: Vector3d(spoint2)

        // ship transform modifies both position in world AND rotation, but while we don't care about position in world,
        // rotation is incredibly important

        val point = rpoint2 - (
            posShipToWorldRender(ship1, spoint1, (ship1.renderTransform as ShipTransformImpl).copy(shipToWorldRotation = rotation)) -
            posShipToWorldRender(ship1, Vector3d(ship1.renderTransform.positionInShip), (ship1.renderTransform as ShipTransformImpl).copy(shipToWorldRotation = rotation))
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
    ): ShipTransform? {
        return null
    }
}