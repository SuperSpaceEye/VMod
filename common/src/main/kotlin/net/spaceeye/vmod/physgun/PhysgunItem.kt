package net.spaceeye.vmod.physgun

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.events.PersistentEvents

class PhysgunItem: Item(Properties().tab(VMItems.TAB).stacksTo(1)) {
    companion object {
        @JvmStatic
        fun playerIsUsingPhysgun(): Boolean {
            val player = Minecraft.getInstance().player ?: return false
            return player.mainHandItem.item == VMItems.PHYSGUN.get().asItem()
        }

        @JvmStatic
        fun makeEvents() {
            PersistentEvents.keyPress.on {
                (keyCode, scanCode, action, modifiers), _ ->
                if (!playerIsUsingPhysgun()) {return@on false}
                if (Minecraft.getInstance().screen != null) {return@on false}

                return@on ClientPhysgunState.handleKeyEvent(keyCode, scanCode, action, modifiers)
            }

            PersistentEvents.mouseMove.on {
                pos, _ ->
                if (!playerIsUsingPhysgun()) {return@on false}
                if (Minecraft.getInstance().screen != null) {return@on false}

                return@on ClientPhysgunState.handleMouseMovement(pos.x, pos.y)
            }

            EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable{
                ClientRawInputEvent.MOUSE_CLICKED_PRE.register {
                        client, button, action, mods ->
                    if (!playerIsUsingPhysgun()) {return@register EventResult.pass()}
                    if (client.screen != null) {return@register EventResult.pass()}

                    return@register ClientPhysgunState.handleMouseButtonEvent(button, action, mods)
                }

                ClientRawInputEvent.MOUSE_SCROLLED.register {
                        client, amount ->
                    if (!playerIsUsingPhysgun()) {return@register EventResult.pass()}
                    if (client.screen != null) {return@register EventResult.pass()}

                    return@register ClientPhysgunState.handleMouseScrollEvent(amount)
                }
                ClientPhysgunState.makeEvents()
            }}
        }
    }
}