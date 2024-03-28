package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.AxisMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import net.spaceeye.vmod.transformProviders.CenteredAroundPlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.CenteredAroundRotationAssistTransformProvider
import net.spaceeye.vmod.transformProviders.PlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.RotationAssistTransformProvider
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

interface AxisCRIHandler: ClientRawInputsHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as AxisMode
        if (primaryStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !secondaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            primaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
            resetState()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as AxisMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlePrimary()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            clientHandleSecondary()
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseScrollEvent(amount: Double): EventResult {
        this as AxisMode
        if (!(primaryStage == ThreeClicksActivationSteps.SECOND_RAYCAST || primaryStage == ThreeClicksActivationSteps.FINALIZATION)) { return EventResult.pass() }

        primaryAngle.it = primaryAngle.it + amount * 0.2

        return EventResult.interruptFalse()
    }

    private fun clientHandleSecondary() {
        this as AxisMode
        secondaryFirstRaycast = !secondaryFirstRaycast
    }

    private fun clientHandlePrimary() {
        this as AxisMode
        when (primaryStage) {
            ThreeClicksActivationSteps.FIRST_RAYCAST  -> clientPrimaryFirst()
            ThreeClicksActivationSteps.SECOND_RAYCAST -> clientPrimarySecond()
            ThreeClicksActivationSteps.FINALIZATION   -> clientPrimaryThird()
        }
    }

    private fun clientPrimaryFirst() {
        this as AxisMode
        if (clientCaughtShip != null) {
            clientCaughtShip!!.transformProvider = null
            clientCaughtShip = null
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

        if (raycastResult.state.isAir) {return}
        val mode = if (posMode != PositionModes.CENTERED_IN_BLOCK) {posMode} else {PositionModes.CENTERED_ON_SIDE}

        clientCaughtShip = (level.getShipManagingPos(raycastResult.blockPosition) ?: return) as ClientShip
        clientCaughtShip!!.transformProvider = PlacementAssistTransformProvider(raycastResult, mode, clientCaughtShip!!)

        primaryStage = ThreeClicksActivationSteps.SECOND_RAYCAST
        return
    }

    private fun clientPrimarySecond() {
        this as AxisMode
        primaryStage = ThreeClicksActivationSteps.FINALIZATION
        if (clientCaughtShip == null) { return }

        val placementTransform = clientCaughtShip!!.transformProvider
        if (placementTransform !is PlacementAssistTransformProvider) {return}

        primaryAngle.it = 0.0
        clientCaughtShip!!.transformProvider = RotationAssistTransformProvider(placementTransform, primaryAngle)

        val shipObjectWorld = Minecraft.getInstance().shipObjectWorld
        clientCaughtShips!!.forEach {
            val ship = shipObjectWorld.allShips.getById(it)
            ship?.transformProvider = CenteredAroundRotationAssistTransformProvider(ship!!.transformProvider as CenteredAroundPlacementAssistTransformProvider)
        }
    }

    private fun clientPrimaryThird() {
        this as AxisMode
        primaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
        if (clientCaughtShip != null) {
            clientCaughtShip!!.transformProvider = null
            clientCaughtShip = null
        }
    }
}