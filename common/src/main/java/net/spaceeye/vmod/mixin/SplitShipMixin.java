package net.spaceeye.vmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.SubShipAssemblyKt;

@Mixin(SubShipAssemblyKt.class)
abstract public class SplitShipMixin {
    @Inject(method = "splitShip", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void vmod_splitShip(
            BlockPos centerBlock, DenseBlockPosSet blocks, ServerLevel level, ServerShip originalShip,
            CallbackInfoReturnable<ServerShip> cir,
            ServerShip ship) {
        AVSEvents.INSTANCE.getSplitShip().emit(new AVSEvents.SplitShipEvent(level, originalShip, ship, centerBlock, blocks));
    }
}
