package net.spaceeye.vmod.config

import net.spaceeye.vmod.PlatformUtils
import net.spaceeye.vmod.VMConfig
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

//TODO rework how this works

private var GLOBAL_DelegateRegisterItemCount = 0
data class DelegateRegisterItem(
    val parentReg: Any?,
    val property: KProperty<*>,
    val description: String,
    val range: Pair<Any, Any>?,
    var resolved_name:String="",
    val do_show: Boolean=true) {
    val counter = GLOBAL_DelegateRegisterItemCount
    init { GLOBAL_DelegateRegisterItemCount++ }

    override fun hashCode(): Int {
        return counter
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
abstract class ConfigSubDirectory

object ConfigDelegateRegister {
    private var default_parameters = hashMapOf<DelegateRegisterItem, Any>()
    private var parameter_range = hashMapOf<DelegateRegisterItem, Pair<Any, Any>?>()
    private var registers = hashMapOf<KProperty<*>, DelegateRegisterItem>()

    private var resolved_get = hashMapOf<String, () -> Any>()
    private var resolved_set = hashMapOf<String, (Any) -> Unit>()

    fun newEntry(entry: DelegateRegisterItem, it: Any) {
        default_parameters[entry] = it
        parameter_range[entry] = entry.range
        registers[entry.property] = entry
    }

    fun getResolved(resolved_name: String): () -> Any = resolved_get[resolved_name]!!
    fun setResolved(resolved_name: String, it: Any) = resolved_set[resolved_name]!!(it)

    private fun getEntry(it: KProperty<*>): DelegateRegisterItem? = registers[it]
    private fun resolveEntry(it: DelegateRegisterItem, configBuilder: AbstractConfigBuilder) {
        val default_parameter = default_parameters[it]!!
        val parameters_range  = parameter_range[it]

        val getSet = if (it.do_show) { configBuilder.makeItem(it.property.name, default_parameter, it.description, parameters_range) }
        else { var local:Any = default_parameter; ConfigValueGetSet({local}, {local = it}) }

        resolved_get[it.resolved_name] = getSet.get
        resolved_set[it.resolved_name] = getSet.set
    }

    private fun reflectResolveConfigPaths(cls: Any, str_path: String, name: String, configBuilder: AbstractConfigBuilder) {
        configBuilder.pushNamespace(name)

        val resolve_later = mutableListOf<Any>()
        val to_resolve = mutableListOf<Pair<DelegateRegisterItem, KProperty1<out Any, *>>>()
        for (item in cls::class.declaredMemberProperties) {
            if (item.visibility != KVisibility.PUBLIC) {continue}
            val entry = getEntry(item)

            if (entry == null) { item.getter.call(cls)?.let { if (it is ConfigSubDirectory) resolve_later.add(it) }; continue }

            to_resolve.add(Pair(entry, item))
        }

        to_resolve.sortBy { it.first.hashCode() }
        for ((entry, item) in to_resolve) {
            entry.resolved_name = str_path + "." + item.name
            resolveEntry(entry, configBuilder)
        }

        for (item in resolve_later) {
            reflectResolveConfigPaths(item, str_path + "." + item::class.simpleName, item::class.simpleName!!, configBuilder)
        }

        configBuilder.popNamespace()
    }

    fun initConfig() {
        VMConfig.server_config_holder = PlatformUtils.getConfigBuilder()
        VMConfig.server_config_holder.beginBuilding()
        reflectResolveConfigPaths(VMConfig.SERVER, "SomePeripheralsConfig", "SomePeripheralsConfig", VMConfig.server_config_holder)
        VMConfig.server_config_holder.finishBuilding("server")

        VMConfig.client_config_holder = PlatformUtils.getConfigBuilder()
        VMConfig.client_config_holder.beginBuilding()
        reflectResolveConfigPaths(VMConfig.CLIENT, "SomePeripheralsConfig", "SomePeripheralsConfig", VMConfig.client_config_holder)
        VMConfig.client_config_holder.finishBuilding("client")

        VMConfig.common_config_holder = PlatformUtils.getConfigBuilder()
        VMConfig.common_config_holder.beginBuilding()
        reflectResolveConfigPaths(VMConfig.COMMON, "SomePeripheralsConfig", "SomePeripheralsConfig", VMConfig.common_config_holder)
        VMConfig.common_config_holder.finishBuilding("common")

        default_parameters.clear()
        parameter_range.clear()
        registers.clear()
    }
}