package net.spaceeye.vmod.schematic.api

import net.spaceeye.vmod.DLOG
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.schematic.api.interfaces.IShipSchematic
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

object SchematicRegistry: Registry<IShipSchematic>() {}

open class Registry<T>(private val useFullNames: Boolean = false) {
    private val typeToSupplier = mutableMapOf<Class<T>, Supplier<T>>()
    private val typeToWrappers = mutableMapOf<Class<T>, MutableList<(T) -> T>>()
    private val suppliers = mutableListOf<Supplier<T>>()

    private var strToIdx = mutableMapOf<String, Int>()
    private var idxToClass = mutableListOf<Class<T>>()

    private fun getName(clazz: Class<*>) = if (useFullNames) clazz.name else clazz.simpleName

    fun register(clazz: Class<T>) {
        if (typeToSupplier.containsKey(clazz)) {throw AssertionError("Type ${getName(clazz)} was already registered")}

        idxToClass.add(clazz)
        strToIdx[getName(clazz)] = idxToClass.size - 1

        val supplier = Supplier {
            var instance = clazz.getConstructor().newInstance()
            typeToWrappers[clazz]?.let { it.forEach { instance = it(instance)!! } }
            instance
        }
        typeToSupplier[clazz] = supplier
        suppliers.add(supplier)
    }

    fun registerWrapper(clazz: Class<T>, wrapper: (item: T) -> T) {
        typeToWrappers.getOrPut(clazz) {mutableListOf()}.add(wrapper)
    }

    fun typeToSupplier(clazz: Class<T>) = typeToSupplier[clazz] ?: throw AssertionError("Type ${clazz.name} wasn't registered")

    fun register(clazz: KClass<*>) {
        // will initialize companion object if exists
        try {
            clazz.companionObjectInstance
        } catch (e: Exception) { WLOG("An exception occurred during initialization of companion object of ${clazz.simpleName} but it probably doesn't mean anything. Stack trace in debug.log."); DLOG("\n${e.stackTraceToString()}")
        } catch (e: Error) { WLOG("An error occurred during initialization of companion object of ${clazz.simpleName} but it probably doesn't mean anything. Stack trace in debug.log."); DLOG("\n${e.stackTraceToString()}")  }
        register(clazz.java as Class<T>)
    }
    fun typeToSupplier(clazz: KClass<*>) = typeToSupplier(clazz.java as Class<T>)
    inline fun <TT: Any> registerWrapper(clazz: KClass<TT>, crossinline wrapper: (item: TT) -> T) = registerWrapper(clazz.java as Class<T>) { item -> wrapper(item as TT) }

    fun typeToString(type: Class<*>): String = getName(type)
    fun strTypeToSupplier(strType: String): Supplier<T> { return typeToSupplier[idxToClass[strToIdx[strType] ?: throw AssertionError("Type $strType wasn't registered")]] ?: throw AssertionError("Type $strType wasn't registered") }

    fun asList() = suppliers.toList()
    fun asTypesList() = idxToClass.toList()
}