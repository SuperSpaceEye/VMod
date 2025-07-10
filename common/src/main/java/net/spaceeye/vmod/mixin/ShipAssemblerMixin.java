package net.spaceeye.vmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.spaceeye.vmod.events.AVSEvents;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;

import java.util.List;

@Mixin(ShipAssembler.class)
abstract class ShipAssemblerMixin {
    @Inject(method = "assembleToShip", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;", shift = At.Shift.AFTER), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    void vmod$eventInject(Level level, List<? extends BlockPos> blocks, boolean removeOriginal, double scale, boolean shouldDisableSplitting, CallbackInfoReturnable<ServerShip> cir, ServerLevel sLevel, LoadedServerShip existingShip, BlockPos structureCornerMin, BlockPos structureCornerMax, boolean hasSolids, Vector3ic contraptionOGPos, Vector3i contraptionWorldPos, Ship newShip, Vector3d contraptionShipPos) {
        AVSEvents.INSTANCE.getBlocksWereMovedEvent().emit(new AVSEvents.BlocksMovedEvent(
                (ServerLevel)level,
                existingShip,
                (ServerShip) newShip,
                contraptionOGPos,
                new Vector3i((int)contraptionShipPos.x, (int)contraptionShipPos.y, (int)contraptionShipPos.z),
                blocks
        ));
    }
}