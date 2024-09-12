package net.spaceeye.vmod.toolgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.events.RandomEvents
import org.lwjgl.glfw.GLFW

class ToolgunItem: Item(Properties().tab(VMItems.TAB).stacksTo(1)) {
    companion object {
        @JvmStatic
        fun playerIsUsingToolgun(): Boolean {
            val player = Minecraft.getInstance().player ?: return false
            return player.mainHandItem.item == VMItems.TOOLGUN.get().asItem()
        }

        @JvmStatic
        fun makeEvents() {
            EnvExecutor.runInEnv(Env.CLIENT) { Runnable {

            ClientGuiEvent.RENDER_HUD.register {
                stack, delta ->
                if (!playerIsUsingToolgun()) {return@register}
                ClientToolGunState.onRenderHUD(stack, delta)
            }

            RandomEvents.keyPress.on {
                (keyCode, scanCode, action, modifiers), _ ->
                if (!playerIsUsingToolgun()) {return@on false}
                if (ClientToolGunState.otherGuiIsOpened()) {return@on false}

                val guiIsOpened = ClientToolGunState.guiIsOpened()
                val isPressed = action == GLFW.GLFW_PRESS

                // we do it like this because we need for toolgun to handle keys first to prevent
                // user from opening menu or smth in the middle of using some mode
                if (!guiIsOpened) {
                    val cancel = ClientToolGunState.handleKeyEvent(keyCode, scanCode, action, modifiers)
                    if (cancel) {return@on true}

                    if (isPressed && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode)) {
                        ClientToolGunState.openGUI()
                        return@on true
                    }
                }

                if (guiIsOpened && isPressed && (ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
                    ClientToolGunState.closeGUI()
                    return@on true
                }

                return@on false
            }

            ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
                client, button, action, mods ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                return@register ClientToolGunState.handleMouseButtonEvent(button, action, mods)
            }

            ClientRawInputEvent.MOUSE_SCROLLED.register {
                client, amount ->
                if (!playerIsUsingToolgun()) {return@register EventResult.pass()}
                if (client.screen != null) {return@register EventResult.pass()}

                return@register ClientToolGunState.handleMouseScrollEvent(amount)
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

            }}
        }
    }
}