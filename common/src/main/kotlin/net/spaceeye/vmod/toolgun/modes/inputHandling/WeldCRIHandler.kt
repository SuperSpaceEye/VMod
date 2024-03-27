package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.WeldMode
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import net.spaceeye.vmod.transformProviders.PlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.RotationAssistTransformProvider
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos

interface WeldCRIHandler: ClientRawInputsHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as WeldMode
        if (secondaryStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !primaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            secondaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
            resetState()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as WeldMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlPrimary()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            clientHandleSecondary()
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseScrollEvent(amount: Double): EventResult {
        this as WeldMode
        if (!(secondaryStage == ThreeClicksActivationSteps.SECOND_RAYCAST || secondaryStage == ThreeClicksActivationSteps.FINALIZATION)) { return EventResult.pass() }

        secondaryAngle.it = secondaryAngle.it + amount * 0.2

        return EventResult.interruptFalse()
    }

    private fun clientHandlPrimary() {
        this as WeldMode
        primaryFirstRaycast = !primaryFirstRaycast
    }

    private fun clientHandleSecondary() {
        this as WeldMode
        when (secondaryStage) {
            ThreeClicksActivationSteps.FIRST_RAYCAST  -> clientSecondaryFirst()
            ThreeClicksActivationSteps.SECOND_RAYCAST -> clientSecondarySecond()
            ThreeClicksActivationSteps.FINALIZATION   -> clientSecondaryThird()
        }
    }

    private fun clientSecondaryFirst() {
        this as WeldMode
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

        caughtShip = (level.getShipManagingPos(raycastResult.blockPosition) ?: return) as ClientShip
        caughtShip!!.transformProvider = PlacementAssistTransformProvider(raycastResult, mode, caughtShip!!)

        secondaryStage = ThreeClicksActivationSteps.SECOND_RAYCAST
        return
    }

    private fun clientSecondarySecond() {
        this as WeldMode
        secondaryStage = ThreeClicksActivationSteps.FINALIZATION
        if (caughtShip == null) { return }

        val placementTransform = caughtShip!!.transformProvider
        if (placementTransform !is PlacementAssistTransformProvider) {return}

        secondaryAngle.it = 0.0
        caughtShip!!.transformProvider = RotationAssistTransformProvider(placementTransform, secondaryAngle)
    }

    private fun clientSecondaryThird() {
        this as WeldMode
        secondaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
        if (caughtShip != null) {
            caughtShip!!.transformProvider = null
            caughtShip = null
        }
    }
}