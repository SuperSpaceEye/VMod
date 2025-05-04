package net.spaceeye.vmod.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityServer;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.Map;
import java.util.Set;

@Mixin(ShipObjectServerWorld.class)
public interface ShipObjectWorldAccessor {
    @Accessor(value = "constraints", remap = false) @NotNull Map<Integer, VSConstraint> getConstraints();
    @Accessor(value = "shipIdToConstraints", remap = false) @NotNull Map<Long, Set<Integer>> getShipIdToConstraints();
    @Accessor(value = "_loadedPhysicsEntities", remap = false) @NotNull Map<Long, PhysicsEntityServer> getShipIdToPhysEntity();
}
