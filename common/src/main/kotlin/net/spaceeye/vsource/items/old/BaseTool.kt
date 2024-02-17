package net.spaceeye.vsource.items.old

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.vsource.VSItems
import net.spaceeye.vsource.utils.RaycastFunctions

//TODO find a way to detect if player it's a singular click or a button press

abstract class BaseTool: Item(Properties().tab(VSItems.TAB).stacksTo(1)) {
    abstract fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)

    abstract fun resetState()

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        if (player.isShiftKeyDown) { resetState(); return super.use(level, player, usedHand) }

        val raycastResult = RaycastFunctions.raycast(level, player, 100.0)

        activatePrimaryFunction(level, player, raycastResult)

        return super.use(level, player, usedHand)
    }
}