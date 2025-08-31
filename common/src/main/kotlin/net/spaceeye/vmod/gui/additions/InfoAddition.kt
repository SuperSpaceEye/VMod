package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.gui.ServersideNetworking
import net.spaceeye.vmod.gui.additions.InfoAdditionNetworking.C2SQueryShipInfo
import net.spaceeye.vmod.gui.additions.InfoAdditionNetworking.S2CShipInfoQueryResponse
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.shipAttachments.CustomMassSave
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.ToolgunKeybinds
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import net.spaceeye.vmod.vEntityManaging.getAllVEntityIdsOfShipId
import net.spaceeye.vmod.vsStuff.VSJointsTracker
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import java.awt.Color
import kotlin.collections.set

class InfoAddition: ScreenWindowAddition() {
    private lateinit var screenContainer: UIContainer
    private var lastShipId = -1L

    private var infoContainer: UIBlock = UIBlock(Color(127, 127, 127, 127))

    private val shipSlug         = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val shipId           = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val mass             = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val gravity          = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val customMass       = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val originalMass     = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val numVSConstraints = makeText("", Color.WHITE, 2f, 2f, infoContainer)
    private val numVEntities     = makeText("", Color.WHITE, 2f, 2f, infoContainer)

    private var lastUpdate = 0L

    private var render = false

    init {
        PersistentEvents.keyPress.on {
            (keyCode, scanCode, action, modifiers), _ ->
            if (!instance.client.playerIsUsingToolgun()) return@on false

            val isPressed = action == GLFW.GLFW_PRESS

            if (isPressed && ToolgunKeybinds.TOOLGUN_TOGGLE_HUD_INFO_KEY.matches(keyCode, scanCode)) {
                render = !render
                return@on true
            }

            return@on false
        }
    }

    override fun init(screenContainer: UIContainer) {
        this.screenContainer = screenContainer

        screenContainer.addChild(infoContainer)
        infoContainer.hide()

        instance.instanceStorage["infoAddition_clientReceiver"] = {pkt: S2CShipInfoQueryResponse -> clientFn(pkt) }
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        if (!render || !instance.client.playerIsUsingToolgun()) {
            infoContainer.hide()
            return
        }

        val level = Minecraft.getInstance().level!!
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val player = Minecraft.getInstance().player!!

        val inFirstPerson = !camera.isDetached

        val dir = if (inFirstPerson) {
            Vector3d(camera.lookVector).snormalize()
        } else {
            Vector3d(player.lookAngle).snormalize()
        }

        val rr = RaycastFunctions.renderRaycast(level,
            RaycastFunctions.Source(
                dir,
                if (inFirstPerson) Vector3d(camera.position) else Vector3d(player.eyePosition)
            ),
            100.0
        )

        if (rr.ship == null) {
            lastShipId = -1
            infoContainer.hide()
            return
        }

        if (lastShipId == rr.shipId && (getNow_ms() - lastUpdate < 1000L)) { return }

        lastShipId = rr.shipId
        queryShipId = rr.shipId

        callback = { Window.enqueueRenderOperation {
            shipSlug        .setText("Slug: ${rr.ship!!.slug}")
            shipId          .setText("ShipId: ${rr.shipId}")
            mass            .setText("Mass: ${it.mass}")
            gravity         .setText("Gravity: ${it.gravity}")
            customMass      .setText("Has custom mass: ${it.customMass}")
            originalMass    .setText("Original mass: ${it.originalMass}")
            numVSConstraints.setText("Num VS Constraints: ${it.numVSConstraints}")
            numVEntities    .setText("Num VEntities: ${it.numVEntities}")

            //TODO simplify
            val maxWidth = (infoContainer.children.maxBy { (it as UIWrappedText).getTextWidth() } as UIWrappedText).let{
                it.getTextWidth() * it.getTextScale() + 4
            }

            infoContainer constrain {
                x = 50.percent
                y = 50.percent

                width = maxWidth.pixels
                height = ChildBasedSizeConstraint() + (8 * 2).pixels
            }

            infoContainer.unhide()
        } }
        c2sQueryShipInfo!!.sendToServer(C2SQueryShipInfo(lastShipId))
    }

    private var queryShipId = -1L
    private var callback: (S2CShipInfoQueryResponse) -> Unit = {}

    var c2sQueryShipInfo: C2SConnection<C2SQueryShipInfo>? = null
        get() {
            if (field != null) return field
        field = instance.instanceStorage["infoAddition_query_ship_info"] as C2SConnection<C2SQueryShipInfo>
        return field
    }

    fun clientFn(pkt: S2CShipInfoQueryResponse) {
        if (queryShipId != pkt.shipId) return
        callback(pkt)
    }
}

object InfoAdditionNetworking: ServersideNetworking {
    data class C2SQueryShipInfo(var shipId: Long = -1L): AutoSerializable
    data class S2CShipInfoQueryResponse(
        var shipId: Long,
        var mass: Double = 0.0,
        var gravity: Vector3d = Vector3d(),
        var customMass: Boolean = false,
        var originalMass: Double = 0.0,
        var numVSConstraints: Int = 0,
        var numVEntities: Int = 0,
    ): AutoSerializable

    override fun initConnections(instance: ToolgunInstance) {
        val s2cShipInfoQueryResponse = regS2C<S2CShipInfoQueryResponse>(instance.modId, "ship_info_query_response", "info_addition") { pkt ->
            (instance.instanceStorage["infoAddition_clientReceiver"] as (S2CShipInfoQueryResponse) -> Unit ).invoke(pkt)
        }

        instance.instanceStorage["infoAddition_ship_info_query_response"] = s2cShipInfoQueryResponse
        instance.instanceStorage["infoAddition_query_ship_info"] = regC2S<C2SQueryShipInfo>(instance.modId, "query_ship_info", "info_addition",
            //TODO sus?
            {pkt, player ->
                val level = player.serverLevel()
                val ship = level.shipObjectWorld.loadedShips.getById(pkt.shipId) ?: return@regC2S false
                ship.transform.positionInWorld.distance(player.position().toJOML()) < 100.0
            }
        ) { pkt, player ->
            val level = player.serverLevel()
            val ship = level.shipObjectWorld.loadedShips.getById(pkt.shipId) ?: return@regC2S

            val customMassSave = CustomMassSave.getOrCreate(ship)
            val changedMassData = customMassSave.massSave
            val originalMass = changedMassData?.let {
                it.fold(ship.inertiaData.mass) { mass, (pos, new) ->
                    val state = level.getBlockState(pos.toBlockPos())
                    val (defaultMass, _) = BlockStateInfo.get(state)!!

                    mass - new + defaultMass
                }
            } ?: ship.inertiaData.mass

            s2cShipInfoQueryResponse.sendToClient(player, S2CShipInfoQueryResponse(
                pkt.shipId,
                ship.inertiaData.mass,
                GravityController.getOrCreate(ship).effectiveGravity(),
                changedMassData != null,
                originalMass,
                VSJointsTracker.getIdsOfShip(ship.id).size,
                level.getAllVEntityIdsOfShipId(ship.id).size
            ))
        }
    }
}