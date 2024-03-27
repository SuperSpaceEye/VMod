package net.spaceeye.vmod.fabric

import net.spaceeye.vmod.config.AbstractConfigBuilder
import net.spaceeye.vmod.config.ConfigValueGetSet

class FabricDummyConfigBuilder: AbstractConfigBuilder() {
    override fun pushNamespace(namespace: String) { }
    override fun popNamespace() { }
    override fun beginBuilding() {}
    override fun finishBuilding(type: String) {}

    override fun <T : Any> makeItem(name: String, defaultValue: T, description: String, range: Pair<T, T>?): ConfigValueGetSet {
        var newVal: T = defaultValue
        return ConfigValueGetSet(
            { newVal },
            { newVal = it as T }
        )
    }
}