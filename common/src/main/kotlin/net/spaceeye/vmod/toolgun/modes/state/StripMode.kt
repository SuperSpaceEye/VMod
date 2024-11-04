package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.toolgun.modes.gui.StripGUI
import net.spaceeye.vmod.toolgun.modes.hud.StripHUD
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.*
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.ceil
import kotlin.math.max

//TODO redo highlighting logic because it feels too complex and convoluted
class StripMode: ExtendableToolgunMode(), StripGUI, StripHUD {
    init {
        RandomEvents.clientPreRender.on {(timestamp), _ ->
            if (!render) {return@on}
            if (!ToolgunItem.playerIsUsingToolgun()) {return@on}

            val minecraft = Minecraft.getInstance()
            val camera = minecraft.gameRenderer.mainCamera

            val result = RaycastFunctions.renderRaycast(minecraft.level!!, RaycastFunctions.Source(Vector3d(camera.lookVector), Vector3d(camera.position)))
            if (result.shipId == -1L) {return@on}
            if (currentQuery != result.shipId) {
                queryIds = null
                currentQuery = result.shipId
                c2sQueryStrippableRendererIds.sendToServer(CS2QueryStrippableRendererIds(currentQuery))
                return@on
            }
            val response = queryIds ?: return@on

            response.forEach {
                val renderer = ClientRenderingData.getItem(it.key) ?: return@forEach
                if (mode == StripModes.StripAll || (it.value - result.globalHitPos!!).sqrDist() <= radius * radius) {
                    renderer.highlightUntil(timestamp+1)
                }
            }
        }
    }

    enum class StripModes {
        StripAll,
        StripInRadius
    }

    var radius: Double by get(0, 1.0, {ServerLimits.instance.stripRadius.get(it)})
    var mode: StripModes by get(1, StripModes.StripAll)

    private var render = false
    override fun eOnOpenMode() {
        render = true
    }

    override fun eOnCloseMode() {
        render = false
    }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        when (mode) {
            StripModes.StripAll -> stripAll(level as ServerLevel, raycastResult)
            StripModes.StripInRadius -> stripInRadius(level as ServerLevel, raycastResult)
        }
    }

    private fun stripAll(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach {
            val mc = level.getManagedConstraint(it)
            if (mc !is ExtendableMConstraint || mc.getExtensionsOfType<Strippable>().isEmpty()) { return@forEach }
            level.removeManagedConstraint(it)
        }
    }

    private fun stripInRadius(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val instance = ConstraintManager.getInstance()

        val b = raycastResult.blockPosition
        val r = max(ceil(radius).toInt(), 1)

        for (x in b.x-r .. b.x+r) {
        for (y in b.y-r .. b.y+r) {
        for (z in b.z-r .. b.z+r) {
            val list = instance.tryGetIdsOfPosition(BlockPos(x, y, z)) ?: continue
            val temp = mutableListOf<ManagedConstraintId>()
            temp.addAll(list)
            temp.forEach {const ->
                val mc = level.getManagedConstraint(const)
                if (mc !is ExtendableMConstraint || mc.getExtensionsOfType<Strippable>().isEmpty()) { return@forEach }
                mc!!.getAttachmentPoints().forEach {
                    if ((it - raycastResult.globalHitPos!!).dist() <= radius) {
                        level.removeManagedConstraint(const)
                    }
                }
            }
        } } }
    }


    companion object {
        var currentQuery: Long = -1
        var queryIds: Map<Int, Vector3d>? = null

        //TODO maybe instead of this create specialized channel for clients to query server?
        data class CS2QueryStrippableRendererIds(var shipId: Long): AutoSerializable
        private val c2sQueryStrippableRendererIds = regC2S<CS2QueryStrippableRendererIds>("query_strippable_renderer_ids", "strip_mode", {ServerToolGunState.playerHasAccess(it)}, {}) {
            pkt, player ->
            val level = ServerLevelHolder.overworldServerLevel!!
            val constraints = level
                .getAllManagedConstraintIdsOfShipId(pkt.shipId)
                .mapNotNull {
                    level.getManagedConstraint(it)?.let {con ->
                if (   con is ExtendableMConstraint
                    && con.getExtensionsOfType<Strippable>().isNotEmpty()
                    && con.getExtensionsOfType<RenderableExtension>().isNotEmpty()
                ) con else null}
            }

            val renderIds = constraints.map { it.getExtensionsOfType<RenderableExtension>().map { it.getRID() } }.flatten().toIntArray()
            val pkt = S2CSendStrippableRendererIds(pkt.shipId, renderIds)
            pkt.positions = constraints.map { it.iGetAttachmentPoints(pkt.shipid)[0] }
            s2cSendStrippableRendererIds.sendToClient(player, pkt)
        }

        data class S2CSendStrippableRendererIds(var shipid: Long, var ids: IntArray): AutoSerializable {
            var positions: List<Vector3d> by get(0, listOf(), {it}, {it, buf -> buf.writeCollection(it){buf, it -> buf.writeVector3d(it)}}) {buf -> buf.readCollection({mutableListOf()}){buf.readVector3d()}}
        }
        private val s2cSendStrippableRendererIds = regS2C<S2CSendStrippableRendererIds>("send_strippable_renderer_ids", "strip_mode") {
            pkt ->
            if (pkt.shipid != currentQuery) {return@regS2C}
            queryIds = pkt.ids.zip(pkt.positions).toMap()
        }

        init {
            ToolgunModes.registerWrapper(StripMode::class) {
                it.addExtension<StripMode> {
                    BasicConnectionExtension<StripMode>("strip_mode"
                        ,primaryFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}