package net.spaceeye.vmod.transformProviders

import net.minecraft.client.Minecraft
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePosition
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.joml.Quaterniond
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.rebuild
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

    @OptIn(VsBeta::class)
    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform? {
        if (!ToolgunItem.playerIsUsingToolgun()) {return null}
        val secondResult = RaycastFunctions.renderRaycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.position)
            ),
            raycastDistance,
            ignoreShipIds
        )
        rresult2 = secondResult

        if (firstResult.globalNormalDirection == null || secondResult.worldNormalDirection == null) { return null }
        // not sure why i need to flip y, but it works
        gdir1 = firstResult .globalNormalDirection!!.also { it.copy().set(it.x, -it.y, it.z) }
        gdir2 = secondResult.globalNormalDirection!!

        if (secondResult.state.isAir) {return null}

        var rotation = (secondResult.ship as ClientShip?)?.renderTransform?.rotation?.get(Quaterniond()) ?: Quaterniond()
            .mul(getQuatFromDir(gdir2))
            .mul(getQuatFromDir(gdir1))
            .normalize()

        spoint1 = getModePosition(mode, firstResult, precisePlacementAssistSideNum)
        spoint2 = getModePosition(mode, secondResult, precisePlacementAssistSideNum)
        val rpoint2 = secondResult.ship?.let {posShipToWorldRender(secondResult.ship as ClientShip, spoint2)} ?: Vector3d(spoint2)

        // ship transform modifies both position in world AND rotation, but while we don't care about position in world,
        // rotation is incredibly important

        val point = rpoint2 - (
            posShipToWorldRender(ship1, spoint1, ship1.renderTransform.rebuild{this.rotation(rotation)}) -
            posShipToWorldRender(ship1, Vector3d(ship1.renderTransform.positionInShip), ship1.renderTransform.rebuild{this.rotation(rotation)})
        )

        return ShipTransformImpl.create(
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