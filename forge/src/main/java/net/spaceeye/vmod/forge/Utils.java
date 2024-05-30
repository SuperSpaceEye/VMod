package net.spaceeye.vmod.forge;

import net.minecraft.server.level.ServerLevel;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class Utils {
    public static ServerShipWorldCore getShipObjectWorld(ServerLevel level) {
        return VSGameUtilsKt.getShipObjectWorld(level);
    }
}
