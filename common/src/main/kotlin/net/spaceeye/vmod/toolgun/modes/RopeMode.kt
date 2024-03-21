package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.ILOG
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.RopeMConstraint
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.IntLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.translate.GUIComponents
import net.spaceeye.vmod.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vmod.translate.GUIComponents.FIXED_DISTANCE
import net.spaceeye.vmod.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vmod.translate.GUIComponents.ROPE
import net.spaceeye.vmod.translate.GUIComponents.SEGMENTS
import net.spaceeye.vmod.translate.GUIComponents.WIDTH
import net.spaceeye.vmod.translate.get
import org.lwjgl.glfw.GLFW

class RopeMode : BaseMode {
    var compliance = 1e-20
    var maxForce = 1e10
    var fixedDistance = -1.0

    var posMode = PositionModes.NORMAL

    var width: Double = .2
    var segments: Int = 16

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeDouble(fixedDistance)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeInt(segments)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        fixedDistance = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        segments = buf.readInt()
    }

    override fun serverSideVerifyLimits() {
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        fixedDistance = ServerLimits.instance.fixedDistance.get(fixedDistance)
    }

    override val itemName = ROPE
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(),     ::compliance,    offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),      ::maxForce,      offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow, limits.fixedDistance)
        makeTextEntry(WIDTH.get(),          ::width,         offset, offset, parentWindow, DoubleLimit(0.0, 1.0))
        makeTextEntry(SEGMENTS.get(),       ::segments,      offset, offset, parentWindow, IntLimit(1, 100))
        makeDropDown(GUIComponents.HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(GUIComponents.NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(GUIComponents.CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(GUIComponents.CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_primary = register { object : C2SConnection<RopeMode>("rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<RopeMode>(context.player, buf, ::RopeMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}

        level.makeManagedConstraint(
            RopeMConstraint(
                shipId1, shipId2,
                compliance,
                spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
                maxForce, dist,
                listOf(prresult.blockPosition, rresult.blockPosition),
                RopeRenderer(
                    ship1 != null,
                    ship2 != null,
                    spoint1, spoint2,
                    dist, width, segments
                )
            )
        ).addFor(player)

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING STATE")
        previousResult = null
    }
}