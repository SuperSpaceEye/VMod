package net.spaceeye.vmod.forge

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import net.spaceeye.vmod.config.AbstractConfigBuilder
import net.spaceeye.vmod.config.ConfigValueGetSet
import kotlin.AssertionError

class ForgeConfigBuilder: AbstractConfigBuilder() {
    val BUILDER = ForgeConfigSpec.Builder()
    var SPEC: ForgeConfigSpec? = null

    override fun pushNamespace(namespace: String) { BUILDER.push(namespace) }
    override fun popNamespace() { BUILDER.pop() }
    override fun beginBuilding() {}
    override fun finishBuilding(type: String) {
        SPEC = BUILDER.build()

        val type = when(type) {
            "client" -> ModConfig.Type.CLIENT
            "server" -> ModConfig.Type.SERVER
            "common" -> ModConfig.Type.COMMON
            else -> throw AssertionError("Invalid config type $type")
        }
        ModLoadingContext.get().registerConfig(type, SPEC, "vmod-$type.toml")
    }

    override fun <T : Any> makeItem(name: String, defaultValue: T, description: String, range: Pair<T, T>?): ConfigValueGetSet {
        if (range == null) {
            val newVal = BUILDER.comment(description).define(name, defaultValue)
            return ConfigValueGetSet(
                { newVal.get() },
                { newVal.set(it as T) }
            )
        } else {
            val part = BUILDER.comment(description)

            //TODO refactor whatever this is
            when (defaultValue) {
                is Int -> {
                    val newVal = part.defineInRange(name, defaultValue as Int, range.first as Int, range.second as Int)
                    return ConfigValueGetSet(
                        { newVal.get() },
                        { newVal.set(it as Int) }
                    )
                }
                is Double -> {
                    val newVal = part.defineInRange(name, defaultValue as Double, range.first as Double, range.second as Double)
                    return ConfigValueGetSet(
                        { newVal.get() },
                        { newVal.set(it as Double) }
                    )
                }
                is Long -> {
                    val newVal = part.defineInRange(name, defaultValue as Long, range.first as Long, range.second as Long)
                    return ConfigValueGetSet(
                        { newVal.get() },
                        { newVal.set(it as Long) }
                    )
                }
                else -> throw AssertionError("Invalid type for defineInRange")
            }
        }
    }
}