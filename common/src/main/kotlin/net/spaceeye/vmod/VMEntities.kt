package net.spaceeye.vmod

import dev.architectury.registry.client.level.entity.EntityRendererRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level
import org.valkyrienskies.mod.client.EmptyRenderer

object VMEntities {
    val ENTITIES: DeferredRegister<EntityType<*>> = DeferredRegister.create(VM.MOD_ID, Registries.ENTITY_TYPE)

    private infix fun <T: Entity>String.withType(entityConstructor: (type: EntityType<T>, level: Level) -> T): RegistrySupplier<EntityType<T>> {
        return ENTITIES.register(this) {
            EntityType.Builder.of(
                    entityConstructor,
                    MobCategory.MISC
            ).sized(.3f, .3f)
                    .build(ResourceLocation(VM.MOD_ID, this).toString())
        }
    }

    fun register() {
        ENTITIES.register()

        EnvExecutor.runInEnv(EnvType.CLIENT) {
            Runnable {
//                EntityRendererRegistry.register({ PHYS_ROPE_COMPONENT.get() }) { context ->
//                    EmptyRenderer(context)
//                }
            }
        }
    }
}