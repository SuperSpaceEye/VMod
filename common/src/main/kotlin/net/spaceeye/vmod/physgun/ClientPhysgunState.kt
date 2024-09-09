package net.spaceeye.vmod.physgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.toJoml
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW

object ClientPhysgunState {
    private var pkt = ServerPhysgunState.C2SPhysgunStateChanged()

    var lastIsHoldingPrimary = false
    var lastIsHoldingRotateKey = false
    var lastPreciseRotation = false

    private fun sendPrimaryState(activate: Boolean): Boolean {
        pkt.primaryActivated = activate

        return true
    }
    private fun sendSecondaryState(activate: Boolean): Boolean {
        pkt.freezeSelected = activate

        return true
    }

    private fun sendTrinaryState(): Boolean{
        pkt.freezeAll = true

        return true
    }

    private fun sendScrollState(amount: Double): EventResult {
        if (!pkt.primaryActivated) {return EventResult.pass()}

        pkt.increaseDistanceBy += amount

        return EventResult.interruptFalse()
    }

    var tempQuat: Quaterniond = Quaterniond()

    internal fun handleKeyEvent(keyCode: Int, scanCode: Int, action: Int, modifiers: Int): Boolean {
        return when (keyCode) {
            GLFW.GLFW_KEY_R -> {
                pkt.unfreezeAllOrOne = action == GLFW.GLFW_PRESS
                pkt.unfreezeAllOrOne
            }
            GLFW.GLFW_KEY_E -> {
                if (pkt.primaryActivated && action == GLFW.GLFW_PRESS) {
                    tempQuat = Minecraft.getInstance().gameRenderer.mainCamera.rotation().toJoml()
                    pkt.quatDiff = Minecraft.getInstance().gameRenderer.mainCamera.rotation().toJoml()
                }
                pkt.rotate = (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) && (pkt.primaryActivated || pkt.rotate)
                pkt.rotate
            }
            GLFW.GLFW_KEY_LEFT_SHIFT -> {
                pkt.preciseRotation = (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) && pkt.rotate
                pkt.preciseRotation
            }
            else -> false
        }

    }

    internal fun handleMouseButtonEvent(button:Int, action:Int, modifiers:Int): EventResult {
        val left = if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            when (action) {
                GLFW.GLFW_PRESS   -> sendPrimaryState(true)
                GLFW.GLFW_RELEASE -> sendPrimaryState(false)
                else -> false
            }
        } else false

        val right = if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            when (action) {
                GLFW.GLFW_PRESS   -> sendSecondaryState(true)
                GLFW.GLFW_RELEASE -> sendSecondaryState(false)
                else -> false
            }
        } else false

        val middle = if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            when (action) {
                GLFW.GLFW_PRESS -> sendTrinaryState()
                GLFW.GLFW_RELEASE -> false
                else -> false
            }
        } else false

        return if (left || right || middle) EventResult.interruptFalse() else EventResult.pass()
    }

    private var lastX = 0.0
    private var lastY = 0.0

    internal fun handleMouseMovement(x: Double, y: Double): Boolean {
        if (!pkt.primaryActivated || !pkt.rotate) {
            this.lastX = x
            this.lastY = y
            return false
        }

        val window = Minecraft.getInstance().window

        val camera = Minecraft.getInstance().gameRenderer.mainCamera

        pkt.quatDiff.rotateAxis((lastX - x) / window.width, Vector3d(camera.upVector).toJomlVector3d())
        pkt.quatDiff.rotateAxis((lastY - y) / window.height, (-Vector3d(camera.leftVector)).toJomlVector3d())

        this.lastX = x
        this.lastY = y
        return true
    }

    internal fun handleMouseScrollEvent(amount: Double): EventResult {
        return sendScrollState(amount)
    }

    private fun send(pkt: ServerPhysgunState.C2SPhysgunStateChanged) {
        lastIsHoldingPrimary = pkt.primaryActivated
        lastIsHoldingRotateKey = pkt.rotate
        lastPreciseRotation = pkt.preciseRotation

        pkt.quatDiff.premul(tempQuat.invert())

        ServerPhysgunState.c2sPrimaryStateChanged.sendToServer(pkt)

        pkt.quatDiff = Minecraft.getInstance().gameRenderer.mainCamera.rotation().toJoml()
        tempQuat = Minecraft.getInstance().gameRenderer.mainCamera.rotation().toJoml()
        pkt.increaseDistanceBy = 0.0
        pkt.freezeAll = false
    }

    fun makeEvents() {
        ClientTickEvent.CLIENT_LEVEL_PRE.register {
            if (   pkt.primaryActivated != lastIsHoldingPrimary
                || pkt.rotate != lastIsHoldingRotateKey
                || pkt.preciseRotation != lastPreciseRotation
                || pkt.freezeAll
                || pkt.freezeSelected
                || pkt.unfreezeAllOrOne
                || pkt.increaseDistanceBy != 0.0
                || pkt.quatDiff != tempQuat
                ) {
                send(pkt)
            }
        }
    }
}