package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.constraintsManaging.addFor
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.constraintsManaging.makeManagedConstraint
import net.spaceeye.vsource.constraintsManaging.types.AxisMConstraint
import net.spaceeye.vsource.guiElements.*
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.get
import org.lwjgl.glfw.GLFW
import java.awt.Color
import net.spaceeye.vsource.limits.DoubleLimit
import net.spaceeye.vsource.limits.ServerLimits
import net.spaceeye.vsource.translate.GUIComponents.AXIS
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vsource.translate.GUIComponents.DISABLE_COLLISIONS
import net.spaceeye.vsource.translate.GUIComponents.FIXED_DISTANCE
import net.spaceeye.vsource.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vsource.translate.GUIComponents.NORMAL
import net.spaceeye.vsource.translate.GUIComponents.WIDTH
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ClientShipTransformProvider
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.sqrt

class AxisTransformProvider(
    var raycastResult: RaycastFunctions.RaycastResult,
    var mode: PositionModes,
    var caughtShip: ClientShip
): ClientShipTransformProvider {
    val level = Minecraft.getInstance().level!!
    val player = Minecraft.getInstance().cameraEntity!!

    override fun provideNextRenderTransform(
        prevShipTransform: ShipTransform,
        shipTransform: ShipTransform,
        partialTick: Double
    ): ShipTransform {
        var raycastResultNew = RaycastFunctions.raycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            )
        )

        val ship = level.getShipManagingPos(raycastResultNew.blockPosition!!)
        if (ship != null && ship.id == caughtShip.id || raycastResultNew.state.isAir) {
            raycastResultNew = RaycastFunctions.raycastNoShips(
                level,
                RaycastFunctions.Source(
                    Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                    Vector3d(Minecraft.getInstance().player!!.eyePosition)
                )
            )
        }

        var rotation = shipTransform.shipToWorldRotation

        if (raycastResult.globalNormalDirection != null) {
            var dir = raycastResult.worldNormalDirection!!
            val up = Vector3d(0, 1, 0)
            val left = Vector3d(1, 0, 0)

            var a = up.cross(dir)
            val d = up.dot(dir)

            val q = if (d < 0.0 && a.sqrDist() <= 0.001) {
                Quaterniond(left.x, left.y, left.z, 0.0)
            } else {
                Quaterniond(a.x, a.y, a.z, sqrt(up.sqrDist() * dir.sqrDist()) + d)
            }

            rotation = q.normalize()
        }

        if (raycastResultNew.globalNormalDirection != null) {
            var dir = raycastResultNew.worldNormalDirection!!
            val up = Vector3d(0, 1, 0)
            val left = Vector3d(1, 0, 0)

            var a = up.cross(dir)
            val d = up.dot(dir)

            val q = if (d < 0.0 && a.sqrDist() <= 0.001) {
                Quaterniond(left.x, left.y, left.z, 0.0)
            } else {
                Quaterniond(a.x, a.y, a.z, sqrt(up.sqrDist() * dir.sqrDist()) + d)
            }

            rotation = q.mul(rotation).normalize()
        }

        return ShipTransformImpl(
            (Vector3d(raycastResultNew.worldHitPos!!)).toJomlVector3d(),
            raycastResult.globalCenteredHitPos!!.toJomlVector3d(),
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

class AxisMode : BaseMode {
    var compliance: Double = 1e-20
    var maxForce: Double = 1e10
    var width: Double = .2

    var fixedDistance: Double = -1.0
    var disableCollisions: Boolean = true

    var posMode = PositionModes.NORMAL

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    var caughtShip: ClientShip? = null

    fun clientHandlePrimary() {
        if (caughtShip != null) {
            caughtShip!!.transformProvider = null
            caughtShip = null
            return
        }

        val raycastResult = RaycastFunctions.raycast(
            Minecraft.getInstance().level!!,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            )
        )

        val level = Minecraft.getInstance().level!!

        if (raycastResult.state.isAir) {return}
        val mode = if (posMode != PositionModes.CENTERED_IN_BLOCK) {posMode} else {PositionModes.CENTERED_ON_SIDE}

        caughtShip = (level.getShipManagingPos(raycastResult.blockPosition!!) ?: return) as ClientShip
        caughtShip!!.transformProvider = AxisTransformProvider(raycastResult, mode, caughtShip!!)
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
            clientHandlePrimary()
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeBoolean(disableCollisions)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        disableCollisions = buf.readBoolean()
    }

    override fun serverSideVerifyLimits() {
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        fixedDistance = ServerLimits.instance.fixedDistance.get(fixedDistance)
    }

    override val itemName = AXIS
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(WIDTH.get(),      ::width,      offset, offset, parentWindow, DoubleLimit(0.0, 1.0))
        makeTextEntry(FIXED_DISTANCE.get(),    ::fixedDistance,     offset, offset, parentWindow)
        makeCheckBox(DISABLE_COLLISIONS.get(), ::disableCollisions, offset, offset, parentWindow)
        makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_secondary = register { object : C2SConnection<AxisMode>("axis_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<AxisMode>(context.player, buf, ::AxisMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }
    val conn_primary   = register { object : C2SConnection<AxisMode>("axis_mode_primary",   "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<AxisMode>(context.player, buf, ::AxisMode) {
            item, serverLevel, player, raycastResult ->

    } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activateSecondaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(AxisMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            fixedDistance,
            disableCollisions,
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(0, 200, 0),
                width
            )
        )).addFor(player)

        resetState()
    }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->
        resetState()
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}