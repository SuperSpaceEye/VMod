package net.spaceeye.vsource

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.platform.Platform
import net.minecraft.client.Minecraft
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.events.LevelEvents
import net.spaceeye.vsource.gui.ExampleGui
import net.spaceeye.vsource.utils.closeClientObjects
import net.spaceeye.vsource.utils.closeServerObjects
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

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

        VSItems.register()

        makeClosingEvents()

//        makeTestGui()
    }

    fun makeTestGui() {
//        val keys = BooleanArray(266)
//        var inited = false
//        ClientTickEvent.CLIENT_POST.register {
//            minecraft ->
//            if (!inited) {
//                gui?.init(minecraft, 1000, 1000)
//                ClientGuiEvent.RENDER_POST.register { screen, poseStack, mouseX, mouseY, delta ->
//                    gui?.render(
//                        poseStack,
//                        mouseX,
//                        mouseY,
//                        delta
//                    )
//                }
////                ClientGuiEvent.RENDER_HUD.register { cli: PoseStack?, tickDelta: Float -> gui.render(cli!!, 0, 0, tickDelta) }
//            }
//            if (!inited) {
//                for (i in 32 until keys.size) keys[i] =
//                    GLFW.glfwGetKey(Minecraft.getInstance().window.window, i) == GLFW.GLFW_PRESS
////                ClientGuiEvent.RENDER_HUD.register { cli: PoseStack?, tickDelta: Float -> gui.render(cli, ) }
//                inited = true
//            }
//
//            for (i in 32 until keys.size) {
//                if (keys[i] != (GLFW.glfwGetKey(Minecraft.getInstance().window.window, i) == GLFW.GLFW_PRESS)) {
//                    keys[i] = !keys[i]
//                    if (keys[i]) {
//                        gui.
//                        if (i == ClickGUIModule.keybind.key) gui.enterGUI()
//                        if (i == HUDEditorModule.keybind.key) gui.enterHUDEditor()
//                        gui.(i)
//                    }
//                }
//            }
//        }
    }

    @JvmStatic
    fun makeClosingEvents() {
        LevelEvents.clientDisconnectEvent.on {
            _, _ ->
            closeClientObjects()
        }

        LevelEvents.serverStopEvent.on {
            _, _ ->
            closeServerObjects()
        }
    }
}