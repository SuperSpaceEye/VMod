package net.spaceeye.vmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon;

@Mixin(ShipDataCommon.class)
abstract class ShipDataCommonMixin {
//    @Inject(method = "getShipAABB")
}
