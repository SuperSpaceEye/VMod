package net.spaceeye.vmod.toolgun.modes.extensions

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.architectury.event.EventResult
import gg.essential.elementa.components.UIContainer
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.addFor
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.networking.S2CSendTraversalInfo
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.transformProviders.CenteredAroundPlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.CenteredAroundRotationAssistTransformProvider
import net.spaceeye.vmod.transformProviders.PlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.RotationAssistTransformProvider
import net.spaceeye.vmod.translate.DISTANCE_FROM_BLOCK
import net.spaceeye.vmod.translate.PLACEMENT_ASSIST_SCROLL_STEP
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.rebuild
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.sign

enum class ThreeClicksActivationSteps {
    FIRST_RAYCAST,
    SECOND_RAYCAST,
    FINALIZATION
}

class PlacementAssistExtension(
    showCenteredInBlock: Boolean,
    override val paNetworkingObject: PlacementAssistNetworking,
    val blockPredicate: (mode: ExtendableToolgunMode) -> Boolean,
    val canUseJoinMode: (inst: ExtendableToolgunMode) -> Boolean,
    override val paVEntityBuilder: (spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double) -> VEntity
): PlacementModesExtension(showCenteredInBlock),
    PlacementAssistClient, PlacementAssistServerPart, AutoSerializable {
    override lateinit var inst: ExtendableToolgunMode

    //TODO clean this up?
    override fun preInit(inst: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.inst = inst
        val exts = inst.getExtensionsOfType<BasicConnectionExtension<*>>()

        if (exts.isEmpty()) { throw AssertionError("PlacementAssistExtension requires BasicConnectionExtension") }

        val ext = inst.getExtensionOfType<BasicConnectionExtension<*>>()

        if (ext.leftFunction != null && ext.rightFunction != null) { throw AssertionError("Both primary and secondary actions of BasicConnectionExtension are used already") }

        val activationFn = {mode: ExtendableToolgunMode, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult ->
            mode.getExtensionOfType<PlacementAssistExtension>().activateFunctionPA(level, player, rr)
        }
        val clientCallback = { mode: ExtendableToolgunMode ->
            mode.getExtensionOfType<PlacementAssistExtension>().clientHandleMouseClickPA()
            mode.refreshHUD()
        }

        if (ext.rightFunction == null) {
            ext.blockRight = blockPredicate
            ext.rightFunction = activationFn
            ext.rightClientCallback = clientCallback
            ext.blockLeft = {mode: ExtendableToolgunMode -> mode.getExtensionOfType<PlacementAssistExtension>().paStage != ThreeClicksActivationSteps.FIRST_RAYCAST}
        } else {
            ext.blockLeft = blockPredicate
            ext.leftFunction = activationFn
            ext.leftClientCallback = clientCallback
            ext.blockRight = {mode: ExtendableToolgunMode -> mode.getExtensionOfType<PlacementAssistExtension>().paStage != ThreeClicksActivationSteps.FIRST_RAYCAST}
        }

        val oldBlockLeft = ext.blockLeft as? (ExtendableToolgunMode) -> Boolean
        val oldBlockRight = ext.blockRight as? (ExtendableToolgunMode) -> Boolean

        ext.blockLeft  = {mode: ExtendableToolgunMode -> oldBlockLeft?.invoke(mode)  ?: false || mode.getExtensionOfType<PlacementAssistExtension>().middleFirstRaycast}
        ext.blockRight = {mode: ExtendableToolgunMode -> oldBlockRight?.invoke(mode) ?: false || mode.getExtensionOfType<PlacementAssistExtension>().middleFirstRaycast}


        val oldMiddle = ext.middleFunction as? (ExtendableToolgunMode, ServerLevel, ServerPlayer, RaycastFunctions.RaycastResult) -> Unit
        val oldMiddleCallback = ext.middleClientCallback as? (ExtendableToolgunMode) -> Unit
        ext.middleFunction = {mode: ExtendableToolgunMode, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult ->
            if (canUseJoinMode(mode)) {
                mode.getExtensionOfType<PlacementAssistExtension>().activateMiddle(level, player, rr)
            } else {
                oldMiddle?.invoke(mode, level, player, rr)
            }
        }
        ext.middleClientCallback = { mode: ExtendableToolgunMode ->
            if (canUseJoinMode(mode)) {
                middleFirstRaycast = !middleFirstRaycast
                mode.refreshHUD()
            } else {
                oldMiddleCallback?.invoke(mode)
            }
        }
        ext.blockMiddle = { mode: ExtendableToolgunMode ->
            !canUseJoinMode(mode) || oldBlockLeft?.invoke(mode) ?: false || oldBlockRight?.invoke(mode) ?: false
        }

        try {
            val bm = inst.getExtensionOfType<BlockMenuOpeningExtension<*>>()
            val oldPredicate = bm.predicate as (ExtendableToolgunMode) -> Boolean
            bm.predicate = {mode: ExtendableToolgunMode -> paStage != ThreeClicksActivationSteps.FIRST_RAYCAST || oldPredicate(mode)}
        } catch (_: AssertionError) {}

        paNetworkingObject.init(inst, type)
    }

    override fun eOnMouseScrollEvent(amount: Double): EventResult {
        if (paStage == ThreeClicksActivationSteps.FIRST_RAYCAST) { return EventResult.pass() }

        paAngle.it += paScrollAngle * amount.sign

        return EventResult.interruptFalse()
    }

    override fun eResetState() {
        paClientResetState()
        paServerResetState()
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        super.eMakeGUISettings(parentWindow)
        makeTextEntry(PLACEMENT_ASSIST_SCROLL_STEP.get(), ::paScrollAngleDeg, 2f, 2f, parentWindow, DoubleLimit())
        makeTextEntry(DISTANCE_FROM_BLOCK.get(), ::paDistanceFromBlock, 2f, 2f, parentWindow, ServerLimits.instance.distanceFromBlock)
    }

    override fun serialize(): FriendlyByteBuf {
        val buf1 = super<PlacementModesExtension>.serialize()
        val buf2 = super<AutoSerializable>.serialize()
        buf1.writeBytes(buf2)
        return buf1
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        super<PlacementModesExtension>.deserialize(buf)
        super<AutoSerializable>.deserialize(buf)
    }

    @JsonIgnore private var i = 0

    override var paDistanceFromBlock: Double by get(i++, 0.0, {ServerLimits.instance.distanceFromBlock.get(it)})
    override var paStage: ThreeClicksActivationSteps by get(i++, ThreeClicksActivationSteps.FIRST_RAYCAST)
    override var paAngle: Ref<Double> by get(i++, Ref(0.0), {it}, customSerialize = { it, buf -> buf.writeDouble((it).it)}, customDeserialize = { buf -> paAngle.it = buf.readDouble(); paAngle})
    override var paScrollAngle: Double by get(i++, Math.toRadians(10.0))
    override var middleFirstRaycast: Boolean by get(i++, false)


    override var paCaughtShip: ClientShip? = null
    override var paCaughtShips: LongArray? = null
    override var paFirstResult: RaycastFunctions.RaycastResult? = null
    override var paSecondResult: RaycastFunctions.RaycastResult? = null
    override var previousResult: RaycastFunctions.RaycastResult? = null
}

interface PlacementAssistClient {
    var paCaughtShip: ClientShip?
    var paCaughtShips: LongArray?
    var paStage: ThreeClicksActivationSteps

    var paAngle: Ref<Double>
    var paScrollAngle: Double
    var paScrollAngleDeg: Double
        get() {return Math.toDegrees(paScrollAngle)}
        set(value) {paScrollAngle = Math.toRadians(value)}

    fun clientHandleMouseClickPA() {
        when (paStage) {
            ThreeClicksActivationSteps.FIRST_RAYCAST  -> clientPlacementAssistFirst()
            ThreeClicksActivationSteps.SECOND_RAYCAST -> clientPlacementAssistSecond()
            ThreeClicksActivationSteps.FINALIZATION   -> clientPlacementAssistThird()
        }
    }

    private fun clientPlacementAssistFirst() {
        this as PlacementAssistExtension
        if (paCaughtShip != null) {
            paClientResetState()
            return
        }

        val raycastResult = RaycastFunctions.raycast(
            Minecraft.getInstance().level!!,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().player!!.eyePosition)
            ),
            VMConfig.CLIENT.TOOLGUN.MAX_RAYCAST_DISTANCE
        )

        val level = Minecraft.getInstance().level!!

        if (raycastResult.state.isAir) {paClientResetState(); return}
        val mode = if (posMode != PositionModes.CENTERED_IN_BLOCK) {posMode} else {PositionModes.CENTERED_ON_SIDE}

        paCaughtShip = (level.getShipManagingPos(raycastResult.blockPosition) ?: run {paClientResetState(); return}) as ClientShip
        paCaughtShip!!.transformProvider = PlacementAssistTransformProvider(raycastResult, mode, paCaughtShip!!, precisePlacementAssistSideNum)

        paStage = ThreeClicksActivationSteps.SECOND_RAYCAST
        return
    }

    private fun clientPlacementAssistSecond() {
        paStage = ThreeClicksActivationSteps.FINALIZATION
        val paCaughtShip = paCaughtShip ?: run { paClientResetState(); return }
        val paCaughtShips = paCaughtShips ?: run { paClientResetState(); return }

        val placementTransform = paCaughtShip.transformProvider
        if (placementTransform !is PlacementAssistTransformProvider) {paClientResetState(); return}

        paAngle.it = 0.0
        try {
            paCaughtShip.transformProvider = RotationAssistTransformProvider(placementTransform, paAngle)
        } catch (e: Exception) { ELOG("HOW TF DID YOU DO THIS???????????????????????????\n${e.stackTraceToString()}"); paClientResetState(); return}

        val shipObjectWorld = Minecraft.getInstance().shipObjectWorld
        paCaughtShips.forEach {
            val ship = shipObjectWorld.allShips.getById(it)
            ship?.transformProvider = CenteredAroundRotationAssistTransformProvider(ship!!.transformProvider as CenteredAroundPlacementAssistTransformProvider)
        }
    }

    private fun clientPlacementAssistThird() {
        paClientResetState()
    }

    fun paClientResetState() {
        paStage = ThreeClicksActivationSteps.FIRST_RAYCAST
        if (paCaughtShip != null) {
            paCaughtShip!!.transformProvider = null
            paCaughtShip = null
        }
        paCaughtShips?.forEach {
            Minecraft.getInstance().shipObjectWorld.allShips.getById(it)?.transformProvider = null
        }
        paCaughtShips = null
    }
}

open class PlacementAssistNetworking(networkName: String): BaseNetworking<ExtendableToolgunMode>() {
    val s2cHandleFailure = regS2C<EmptyPacket>("handle_failure", networkName) {
        clientObj?.resetState()
    }

    // Client has no information about VEntities, so server should send it to the client
    val s2cSendTraversalInfo = regS2C<S2CSendTraversalInfo>("send_traversal_info", networkName) {pkt->
        val mobj = clientObj!!
        val obj = mobj.getExtensionOfType<PlacementAssistExtension>()

        if (obj.paCaughtShip == null) { return@regS2C }

        if (obj.paCaughtShips != null) {
            obj.paCaughtShips?.forEach {
                Minecraft.getInstance().shipObjectWorld.allShips.getById(it)?.transformProvider = null
            }
        }

        val primaryTransform = obj.paCaughtShip!!.transformProvider
        if (primaryTransform !is PlacementAssistTransformProvider) { return@regS2C }

        obj.paCaughtShips = pkt.data.filter { it != obj.paCaughtShip!!.id }.toLongArray()
        val clientShipObjectWorld = Minecraft.getInstance().shipObjectWorld

        obj.paCaughtShips!!.forEach {
            val ship = clientShipObjectWorld.allShips.getById(it)
            ship?.transformProvider = CenteredAroundPlacementAssistTransformProvider(primaryTransform, ship!!)
        }
    }
}

interface PlacementAssistServerPart {
    var paStage: ThreeClicksActivationSteps
    var precisePlacementAssistSideNum: Int
    var posMode: PositionModes

    var paAngle: Ref<Double>

    var paFirstResult: RaycastFunctions.RaycastResult?
    var paSecondResult: RaycastFunctions.RaycastResult?

    //TODO change stuff to floats
    val paVEntityBuilder: (spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double) -> VEntity
    val paNetworkingObject: PlacementAssistNetworking

    val paDistanceFromBlock: Double


    var middleFirstRaycast: Boolean
    var previousResult: RaycastFunctions.RaycastResult?
    val inst: ExtendableToolgunMode


    fun activateMiddle(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = _serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || middleFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, {inst.resetState()}) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        if (previousResult!!.ship == null) {
            paFirstResult = rresult
            paSecondResult = previousResult
        } else {
            paFirstResult = previousResult
            paSecondResult = rresult
        }

        val paFirstResult = paFirstResult!!
        val paSecondResult = paSecondResult!!

        val ship1 = level.getShipManagingPos(paFirstResult.blockPosition)
        val ship2 = level.getShipManagingPos(paSecondResult.blockPosition)

        if (ship1 == null)  {return@_serverRaycast2PointsFnActivation handleFailure(player)}
        if (ship1 == ship2) {return@_serverRaycast2PointsFnActivation handleFailure(player)}

        val dir2 = paSecondResult.worldNormalDirection!!

        val (spoint1, spoint2) = getModePositions(if (posMode == PositionModes.CENTERED_IN_BLOCK) {PositionModes.CENTERED_ON_SIDE} else {posMode}, paFirstResult, paSecondResult, precisePlacementAssistSideNum)
        var rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

        rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))
        val rpoint1 = Vector3d(rpoint2) + dir2.normalize() * paDistanceFromBlock

        val shipId1: ShipId = ship1.id
        val shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val ventity = paVEntityBuilder(spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2, Pair(paFirstResult, paSecondResult), paDistanceFromBlock)
        level.makeVEntity(ventity){it.addFor(player)}
        paServerResetState()
    }

    fun activateFunctionPA(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level !is ServerLevel) {throw RuntimeException("Function intended for server use only was activated on client. How.")}
        when (paStage) {
            ThreeClicksActivationSteps.SECOND_RAYCAST -> paFunctionFirst (level, player, raycastResult)
            ThreeClicksActivationSteps.FINALIZATION   -> paFunctionSecond(level, player, raycastResult)
            ThreeClicksActivationSteps.FIRST_RAYCAST  -> paFunctionThird (level, player, raycastResult)
        }
    }

    private fun handleFailure(player: Player) {
        paNetworkingObject.s2cHandleFailure.sendToClient(player as ServerPlayer, EmptyPacket())
        paServerResetState()
    }

    private fun paFunctionFirst(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return handleFailure(player)}
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return handleFailure(player)
        paFirstResult = raycastResult
        val traversed = traverseGetConnectedShips(ship.id).traversedShipIds
        paNetworkingObject.s2cSendTraversalInfo.sendToClient(player as ServerPlayer, S2CSendTraversalInfo(traversed))
    }

    private fun paFunctionSecond(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return handleFailure(player) }
        val ship = level.getShipManagingPos(raycastResult.blockPosition)
        if (ship == level.getShipManagingPos(paFirstResult?.blockPosition ?: return handleFailure(player))) {return handleFailure(player)}
        paSecondResult = raycastResult
    }

    @OptIn(VsBeta::class)
    private fun paFunctionThird(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (paFirstResult == null || paSecondResult == null) {return handleFailure(player)}

        val paFirstResult = paFirstResult!!
        val paSecondResult = paSecondResult!!

        val ship1 = level.getShipManagingPos(paFirstResult.blockPosition)
        val ship2 = level.getShipManagingPos(paSecondResult.blockPosition)

        if (ship1 == null) {return handleFailure(player)}
        if (ship1 == ship2) {return handleFailure(player)}

        // not sure why i need to flip y, but it works
        val dir1 = paFirstResult .globalNormalDirection!!.also { it.copy().set(it.x, -it.y, it.z) }
        val dir2 = paSecondResult.globalNormalDirection!!

        val rotation = (ship2?.transform?.rotation?.get(Quaterniond()) ?: Quaterniond())
            .mul(Quaterniond(AxisAngle4d(paAngle.it, dir2.toJomlVector3d())))
            .mul(getQuatFromDir(dir2))
            .mul(getQuatFromDir(dir1))
            .normalize()


        val (spoint1, spoint2) = getModePositions(if (posMode == PositionModes.CENTERED_IN_BLOCK) {PositionModes.CENTERED_ON_SIDE} else {posMode}, paFirstResult, paSecondResult, precisePlacementAssistSideNum)
        var rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

        // rotation IS IMPORTANT, so make a new transform with new rotation to translate points
        val newTransform = ship1.transform.rebuild {
            this.rotation(rotation)
        }

        // we cannot modify position in ship, we can, however, modify position in world
        // this translates ship so that after teleportation its spoint1 will be at rpoint2
        val point = rpoint2 - (
            posShipToWorld(ship1, spoint1, newTransform) - Vector3d(ship1.transform.positionInWorld)
        ) + dir2.normalize() * paDistanceFromBlock

        teleportShipWithConnected(level, ship1, point, rotation)

        rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))
        val rpoint1 = Vector3d(rpoint2) + dir2.normalize() * paDistanceFromBlock

        val shipId1: ShipId = ship1.id
        val shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val ventity = paVEntityBuilder(spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2, Pair(paFirstResult, paSecondResult), paDistanceFromBlock)
        level.makeVEntity(ventity){it.addFor(player)}
        paServerResetState()
    }
    fun paServerResetState() {
        paStage = ThreeClicksActivationSteps.FIRST_RAYCAST
        paFirstResult = null
        paSecondResult = null
        previousResult = null
        middleFirstRaycast = false
    }
}