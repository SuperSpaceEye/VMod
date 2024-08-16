package net.spaceeye.vmod.utils

import java.util.function.Supplier
import kotlin.reflect.KClass

interface RegistryObject {
    val typeName: String
}

open class ClassRegistry<T> {
    private val typeToConstructor = mutableMapOf<Class<T>, Supplier<T>>()
    private val typeToWrappers = mutableMapOf<Class<T>, MutableList<(T) -> T>>()
    private val suppliers = mutableListOf<Supplier<T>>()
    fun register(clazz: Class<T>) {
        if (typeToConstructor.containsKey(clazz)) {throw AssertionError("Type ${clazz.name} was already registered")}
        val supplier = Supplier {
            var instance = clazz.getConstructor().newInstance()
            typeToWrappers[clazz]?.let { it.forEach { instance = it(instance)!! } }
            instance
        }
        typeToConstructor[clazz] = supplier
        suppliers.add(supplier)
    }

    fun registerWrapper(clazz: Class<T>, wrapper: (item: T) -> T) {
        typeToWrappers.getOrPut(clazz) {mutableListOf()}.add(wrapper)
    }

    fun typeToSupplier(clazz: Class<T>)
        = typeToConstructor[clazz] ?: throw AssertionError("Type ${clazz.name} wasn't registered")

    fun <TT: Any> register(clazz: KClass<TT>) = register(clazz.java as Class<T>)
    fun <TT: Any> typeToSupplier(clazz: KClass<TT>) = typeToSupplier(clazz.java as Class<T>)
    fun <TT: Any> registerWrapper(clazz: KClass<TT>, wrapper: (item: T) -> T) = registerWrapper(clazz.java as Class<T>, wrapper)

    fun asList() = suppliers.toList()
}

open class Registry<T: RegistryObject> {
    private val strToIdx = mutableMapOf<String, Int>()
    private val idxToStr = mutableMapOf<Int, String>()
    private val suppliers = mutableListOf<Supplier<T>>()

    fun register(supplier: Supplier<T>) {
        val type = supplier.get().typeName
        if (strToIdx[type] != null) {throw AssertionError("Type $type was already registered")}

        suppliers.add(supplier)
        strToIdx[type] = suppliers.size - 1
        idxToStr[suppliers.size - 1] = type
    }

    fun typeToSupplier(type: String) = suppliers[strToIdx[type]!!]
    fun asList() = suppliers.toList()

    fun typeToIdx(type: String) = strToIdx[type]
    fun idxToSupplier(idx: Int) = suppliers[idx]
    fun getSchema(): Map<String, Int> = strToIdx
    fun setSchema(schema: Map<Int, String>) {
        if (!schema.values.containsAll(strToIdx.keys)) { throw AssertionError("Schemas are incompatible ") }
        schema.forEach {(k, v) -> strToIdx[v] = k}
    }
}