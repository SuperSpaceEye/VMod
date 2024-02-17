package net.spaceeye.vsource.items

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.VSItems
import net.spaceeye.vsource.WLOG
import net.spaceeye.vsource.gui.ExampleGui
import net.spaceeye.vsource.gui.ToolGunGUI
import net.spaceeye.vsource.items.old.BaseTool
import net.spaceeye.vsource.toolgun.ClientToolGunState
import net.spaceeye.vsource.utils.RaycastFunctions
import org.lwjgl.glfw.GLFW

class Toolgun: Item(Properties().tab(VSItems.TAB).stacksTo(1)) {

//    override fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
//        if (level !is ClientLevel) {return}
//
//        val gui = ToolGunGUI()
//
//        Minecraft.getInstance().setScreen(gui)
//    }

    companion object {
        @JvmStatic
        fun playerIsUsingToolgun(): Boolean {
            val player = Minecraft.getInstance().player ?: return false
            return player.mainHandItem.item == VSItems.TOOLGUN.get().asItem()
        }

        @JvmStatic
        fun makeEvents() {
            ClientRawInputEvent.KEY_PRESSED.register {
                client, keyCode, scanCode, action, modifiers ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}

                val guiIsOpened = ClientToolGunState.guiIsOpened()

                if (!guiIsOpened && keyCode == ClientToolGunState.guiOpenKey && action == GLFW.GLFW_PRESS) {
                    Minecraft.getInstance().setScreen(ClientToolGunState.gui)
                    return@register EventResult.interruptDefault()
                }
                if (guiIsOpened && keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
                    Minecraft.getInstance().setScreen(null)
                    return@register EventResult.interruptDefault()
                }
                
                if (guiIsOpened) {
                    val gui = ClientToolGunState.gui
                    when (action) {
                        GLFW.GLFW_PRESS -> gui.keyPressed(keyCode, scanCode, modifiers)
                        GLFW.GLFW_REPEAT -> gui.keyPressed(keyCode, scanCode, modifiers)
                        GLFW.GLFW_RELEASE -> gui.keyReleased(keyCode, scanCode, modifiers)
                    }
                    EventResult.interruptDefault()
                }

                EventResult.pass()
            }

            ClientLifecycleEvent.CLIENT_STARTED.register {
                ClientToolGunState.init()
            }
        }
    }
}