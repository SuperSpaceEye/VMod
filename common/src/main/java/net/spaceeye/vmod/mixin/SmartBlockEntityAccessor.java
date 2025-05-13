package net.spaceeye.vmod.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmartBlockEntity.class)
public interface SmartBlockEntityAccessor {
    @Accessor(value = "firstNbtRead", remap = false) void vmod$setFirstNbtRead(boolean value);
}
