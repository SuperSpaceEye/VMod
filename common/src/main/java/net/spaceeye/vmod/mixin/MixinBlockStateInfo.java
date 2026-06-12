package net.spaceeye.vmod.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Objects;

@Mixin(BlockStateInfo.class)
public class MixinBlockStateInfo {
//    @Unique
//    int vmod$posX = 0;
//    @Unique int vmod$posY = 0;
//    @Unique int vmod$posZ = 0;
//    @Unique String vmod$dimensionId = "";
//    @Unique VsiBlockType vmod$oldBT = null;
//    @Unique VsiBlockType vmod$newBT = null;
//
//    @Inject(method = "onSetBlock(Lnet/minecraft/world/level/Level;IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At(value = "HEAD"), remap = false)
//    void vmod$onSetBlock(Level level, int x, int y, int z, BlockState prevBlockState, BlockState newBlockState, CallbackInfo ci) {
//        vmod$posX = x;
//        vmod$posY = y;
//        vmod$posZ = z;
//        vmod$dimensionId = VSGameUtilsKt.getDimensionId(level);
//        vmod$oldBT = BlockStateInfo.INSTANCE.get(prevBlockState).getSecond();
//        vmod$newBT = BlockStateInfo.INSTANCE.get(newBlockState).getSecond();
//    }
//
//    @ModifyArg(method = "onSetBlock(Lnet/minecraft/world/level/Level;IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
//            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/internal/world/VsiShipWorld;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
//            remap = false,
//            index = 6
//    )
//    double vmod$onSetBlock(double oldBlockMass) {
//        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
//        if (vmod$oldBT != vmod$newBT) {
//            CustomBlockMassManager.INSTANCE.removeCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
//        }
//        return Objects.requireNonNullElse(mass, oldBlockMass);
//    }

    //VS uses old as balls mixin extras that doesn't have "Args" so i have to do above retardation
//    @ModifyArgs(
//            method = "onSetBlock(Lnet/minecraft/world/level/Level;IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
//            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/internal/world/VsiShipWorld;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
//            remap = false)
//    void vmod$onSetBlock(Args args) {
//        var x = (int)args.get(0);
//        var y = (int)args.get(1);
//        var z = (int)args.get(2);
//        var dimensionId = (String)args.get(3);
//        var oldBT = (VsiBlockType)args.get(4);
//        var newBT = (VsiBlockType)args.get(5);
//        var oldMass = (double)args.get(6);
//
//        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, x, y, z);
//        if (oldBT != newBT) {
//            CustomBlockMassManager.INSTANCE.removeCustomMass(dimensionId, x, y, z);
//        }
//
//        args.set(6, Objects.requireNonNullElse(mass, oldMass));
//    }
}
