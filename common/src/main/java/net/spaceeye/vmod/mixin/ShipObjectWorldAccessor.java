package net.spaceeye.vmod.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.Map;

@Mixin(ShipObjectServerWorld.class)
public interface ShipObjectWorldAccessor {
    @Accessor("constraints") @NotNull Map<Integer, VSConstraint> getConstraintsAcc();
}
