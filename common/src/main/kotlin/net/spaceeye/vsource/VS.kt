package net.spaceeye.vsource

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.spaceeye.vsource.items.Toolgun
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.toolgun.ToolgunModes
import net.spaceeye.vsource.utils.ServerLevelHolder
import net.spaceeye.vsource.utils.closeClientObjects
import net.spaceeye.vsource.utils.closeServerObjects
import net.spaceeye.vsource.utils.constraintsSaving.ConstraintManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = VS.logger.info(s)
fun WLOG(s: String) = VS.logger.warn(s)
fun DLOG(s: String) = VS.logger.debug(s)

object VS {
    const val MOD_ID = "vsource"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("valkyrienskies")) {
            WLOG("VALKYRIEN SKIES IS NOT INSTALLED. NOT INITIALIZING THE MOD.")
            return
        }
        SynchronisedRenderingData
        ToolgunModes

        VSItems.register()

        makeEvents()

        makeTest()
    }

    fun makeTest() {
//        ClientRawInputEvent.KEY_PRESSED.register {
//                client, keyCode, scanCode, action, modifiers ->
//            WLOG("${keyCode} ${scanCode} ${action} ${modifiers}")
//            if (keyCode == GLFW.GLFW_KEY_E && action == GLFW.GLFW_PRESS) {
//                WLOG("PRESSING E HOLY SHIT")
//            }
//            EventResult.pass()
//        }
//
//        ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
//            minecraft, button, action, mods->
//
//            val sbutton = when(button) {
//                GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LEFT"
//                GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MIDDLE"
//                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RIGHT"
//                else -> "unknown"
//            }
//
//            val saction = when(action) {
//                GLFW.GLFW_PRESS -> "PRESSED"
//                GLFW.GLFW_RELEASE -> "RELEASED"
//                2 -> "BEING PRESSED"
//                else -> "unknown"
//            }
//
//            WLOG("Button ${sbutton} IS ${saction} ${mods}")
//
//            EventResult.pass()
//        }
    }

    @JvmStatic
    fun makeEvents() {
        ClientLifecycleEvent.CLIENT_STOPPING.register {
            closeClientObjects()
        }

        LifecycleEvent.SERVER_STOPPING.register {
            closeServerObjects()
        }

        LifecycleEvent.SERVER_STARTED.register {
            server ->
            ServerLevelHolder.server = server
            ServerLevelHolder.serverLevel = server.overworld()
            ConstraintManager.forceNewInstance(server.overworld())
        }

        Toolgun.makeEvents()
    }
}