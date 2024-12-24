package net.spaceeye.vmod.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.shadow.DF;

import java.util.Map;
import java.util.Set;

@Mixin(DF.class)
public interface ShipObjectWorldAccessor {
    @Accessor("D") @NotNull Map<Integer, VSConstraint> getConstraints();
    @Accessor("E") @NotNull Map<Long, Set<Integer>> getShipIdToConstraints();
}
