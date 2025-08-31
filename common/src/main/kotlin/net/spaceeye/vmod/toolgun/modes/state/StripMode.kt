package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.toolgun.modes.gui.StripGUI
import net.spaceeye.vmod.toolgun.modes.hud.StripHUD
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.toolgun.VMToolgun
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
        PersistentEvents.clientPreRender.on { (timestamp), _ ->
            if (!render) {return@on}
            if (!instance.client.playerIsUsingToolgun()) {return@on}

            val minecraft = Minecraft.getInstance()
            val camera = minecraft.gameRenderer.mainCamera

            val result = RaycastFunctions.renderRaycast(minecraft.level!!, RaycastFunctions.Source(Vector3d(camera.lookVector), Vector3d(camera.position)))
            if (result.shipId == -1L) {
                currentQuery = -1
                return@on
            }
            if (currentQuery != result.shipId) {
                queryIds = null
                currentQuery = result.shipId
                c2sQueryStrippableRendererIds.sendToServer(CS2QueryStrippableRendererIds(currentQuery))
                return@on
            }
            val response = queryIds ?: return@on

            response.forEach {
                val renderer = RenderingData.client.getItem(it.key) ?: return@forEach
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
    @JsonIgnore private var i = 0

    var radius: Double by get(i++, 1.0) { ServerLimits.instance.stripRadius.get(it) }
    var mode: StripModes by get(i++, StripModes.StripAll)

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

        level.getAllVEntityIdsOfShipId(ship.id).forEach {
            val mc = level.getVEntity(it)
            if (mc !is ExtendableVEntity || mc.getExtensionsOfType<Strippable>().isEmpty()) { return@forEach }
            level.removeVEntity(it)
        }
    }

    private fun stripInRadius(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val b = raycastResult.blockPosition
        val r = max(ceil(radius).toInt(), 1)

        for (x in b.x-r .. b.x+r) {
        for (y in b.y-r .. b.y+r) {
        for (z in b.z-r .. b.z+r) {
            val list = level.getVEntityIdsOfPosition(BlockPos(x, y, z)) ?: continue
            val temp = mutableListOf<VEntityId>()
            temp.addAll(list)
            temp.forEach {const ->
                val mc = level.getVEntity(const)
                if (mc !is ExtendableVEntity || mc.getExtensionsOfType<Strippable>().isEmpty()) { return@forEach }
                mc.getAttachmentPoints().forEach {
                    if ((it - raycastResult.globalHitPos!!).dist() <= radius) {
                        level.removeVEntity(const)
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
        private val c2sQueryStrippableRendererIds = regC2S<CS2QueryStrippableRendererIds>(MOD_ID, "query_strippable_renderer_ids", "strip_mode", { pkt, player -> VMToolgun.server.playerHasAccess(player)}) {
            pkt, player ->
            val level = ServerObjectsHolder.overworldServerLevel!!
            val ventities = level
                .getAllVEntityIdsOfShipId(pkt.shipId)
                .mapNotNull {
                    level.getVEntity(it)?.let { con ->
                if (   con is ExtendableVEntity
                    && con.getExtensionsOfType<Strippable>().isNotEmpty()
                    && con.getExtensionsOfType<RenderableExtension>().isNotEmpty()
                ) con else null}
            }

            val renderIds = ventities.map { it.getExtensionsOfType<RenderableExtension>().map { it.rID } }.flatten().toIntArray()
            val pkt = S2CSendStrippableRendererIds(pkt.shipId, renderIds)
            pkt.positions = ventities.map { it.iGetAttachmentPoints(pkt.shipid)[0] }
            s2cSendStrippableRendererIds.sendToClient(player, pkt)
        }

        data class S2CSendStrippableRendererIds(var shipid: Long, var ids: IntArray): AutoSerializable {
            @JsonIgnore private var i = 0

            var positions: List<Vector3d> by get(i++, listOf(), {it}, { it, buf -> buf.writeCollection(it){ buf, it -> buf.writeVector3d(it)}}) { buf -> buf.readCollection({mutableListOf()}){buf.readVector3d()}}
        }
        private val s2cSendStrippableRendererIds = regS2C<S2CSendStrippableRendererIds>(MOD_ID, "send_strippable_renderer_ids", "strip_mode") {
            pkt ->
            if (pkt.shipid != currentQuery) {return@regS2C}
            queryIds = pkt.ids.zip(pkt.positions).toMap()
        }

        init {
            ToolgunModes.registerWrapper(StripMode::class) {
                it.addExtension {
                    BasicConnectionExtension<StripMode>("strip_mode"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}