package net.spaceeye.vsource.toolgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import net.spaceeye.vsource.VSItems
import org.lwjgl.glfw.GLFW

class ToolgunItem: Item(Properties().tab(VSItems.TAB).stacksTo(1)) {
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
                if (guiIsOpened && keyCode == GLFW.GLFW_KEY_TAB) {
                    Minecraft.getInstance().setScreen(null)
                    return@register EventResult.interruptDefault()
                }

                return@register ClientToolGunState.handleKeyEvent(keyCode, scanCode, action, modifiers)
            }

            ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
                client, button, action, mods ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                ClientToolGunState.handleMouseButtonEvent(button, action, mods)

                return@register EventResult.interruptDefault()
            }

            ClientLifecycleEvent.CLIENT_STARTED.register {
                ClientToolGunState.init()
            }
        }
    }
}