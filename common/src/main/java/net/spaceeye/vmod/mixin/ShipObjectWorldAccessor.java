package net.spaceeye.vmod.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.apigame.joints.VSJoint;
import org.valkyrienskies.core.impl.shadow.DO;
import org.valkyrienskies.core.impl.shadow.Ep;

import java.util.Map;
import java.util.Set;

@Mixin(Ep.class)
public interface ShipObjectWorldAccessor {
    @Accessor("E") @NotNull Map<Integer, VSJoint> getConstraints();
    @Accessor("F") @NotNull Map<Long, Set<Integer>> getShipIdToConstraints();
    @Accessor("d") @NotNull Map<String, DO> getDimensionState();  //in updateDimension last line
}
