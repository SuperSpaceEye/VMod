package net.spaceeye.vsource.items

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import net.spaceeye.vsource.VSItems
import net.spaceeye.vsource.toolgun.ClientToolGunState
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

                EventResult.pass()
            }

            ClientLifecycleEvent.CLIENT_STARTED.register {
                ClientToolGunState.init()
            }
        }
    }
}