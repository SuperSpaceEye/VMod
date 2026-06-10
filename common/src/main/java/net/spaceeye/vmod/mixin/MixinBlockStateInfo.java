package net.spaceeye.vmod.mixin;

import net.minecraft.world.level.Level;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;

import java.util.Objects;

@Mixin(BlockStateInfo.class)
public class MixinBlockStateInfo {
    @ModifyArgs(
            method = "onSetBlock(Lnet/minecraft/world/level/Level;IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/internal/world/VsiShipWorld;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
            remap = false)
    void vmod$onSetBlock(Args args) {
        var x = (int)args.get(0);
        var y = (int)args.get(1);
        var z = (int)args.get(2);
        var dimensionId = (String)args.get(3);
        var oldBT = (VsiBlockType)args.get(4);
        var newBT = (VsiBlockType)args.get(5);
        var oldMass = (double)args.get(6);

        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, x, y, z);
        if (oldBT != newBT) {
            CustomBlockMassManager.INSTANCE.removeCustomMass(dimensionId, x, y, z);
        }

        args.set(6, Objects.requireNonNullElse(mass, oldMass));
    }
}
