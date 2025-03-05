package net.spaceeye.vmod.mixin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.spaceeye.vmod.VMConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.mod.common.command.VSCommands;
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource;

@Mixin(VSCommands.class)
public abstract class VSCommandsMixin {
    @Inject(method = "literal", at = @At("RETURN"), cancellable = true, remap = false)
    void vmod$addRequirement(String name, CallbackInfoReturnable<LiteralArgumentBuilder<VSCommandSource>> cir) {
        if (!name.equals("vs")) {return;}
        var literal = cir.getReturnValue();
        var newResult = literal.requires((it) -> ((CommandSourceStack)it).hasPermission(VMConfig.INSTANCE.getSERVER().getPERMISSIONS().getVS_COMMANDS_PERMISSION_LEVEL()));
        cir.setReturnValue(newResult);
    }
}
