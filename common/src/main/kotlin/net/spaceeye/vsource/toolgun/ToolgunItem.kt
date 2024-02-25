package net.spaceeye.vsource.toolgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientPlayerEvent
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
                if (ClientToolGunState.otherGuiIsOpened()) {return@register EventResult.pass()}

                val guiIsOpened = ClientToolGunState.guiIsOpened()
                val isPressed = action == GLFW.GLFW_PRESS

                if (!guiIsOpened && isPressed && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode)) {
                    Minecraft.getInstance().setScreen(ClientToolGunState.gui)
                    return@register EventResult.pass()
                }
                if ( guiIsOpened && isPressed && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode)) {
                    Minecraft.getInstance().setScreen(null)
                    return@register EventResult.pass()
                }

                return@register ClientToolGunState.handleKeyEvent(keyCode, scanCode, action, modifiers)
            }

            ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
                client, button, action, mods ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                return@register ClientToolGunState.handleMouseButtonEvent(button, action, mods)
            }

            var inited = false
            ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register {
                if (inited) { return@register }
                ClientToolGunState.init()
                inited = true
            }
            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
                if (it != Minecraft.getInstance().player || it == null) {return@register}
                inited = false
            }
        }
    }
}