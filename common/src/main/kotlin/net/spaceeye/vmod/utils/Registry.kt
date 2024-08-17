package net.spaceeye.vmod.utils

import java.util.function.Supplier
import kotlin.reflect.KClass

interface RegistryObject {
    val typeName: String
}

//TODO revise?
open class ClassRegistry<T> {
    private val typeToSupplier = mutableMapOf<Class<T>, Supplier<T>>()
    private val typeToWrappers = mutableMapOf<Class<T>, MutableList<(T) -> T>>()
    private val suppliers = mutableListOf<Supplier<T>>()

    private val strToIdx = mutableMapOf<String, Int>()
    private val idxToClass = mutableListOf<Class<T>>()

    fun register(clazz: Class<T>) {
        if (typeToSupplier.containsKey(clazz)) {throw AssertionError("Type ${clazz.name} was already registered")}

        idxToClass.add(clazz)
        strToIdx[clazz.name] = idxToClass.size - 1

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

    fun typeToSupplier(clazz: Class<T>)
        = typeToSupplier[clazz] ?: throw AssertionError("Type ${clazz.name} wasn't registered")

    fun <TT: Any> register(clazz: KClass<TT>) = register(clazz.java as Class<T>)
    fun <TT: Any> typeToSupplier(clazz: KClass<TT>) = typeToSupplier(clazz.java as Class<T>)
    fun <TT: Any> registerWrapper(clazz: KClass<TT>, wrapper: (item: T) -> T) = registerWrapper(clazz.java as Class<T>, wrapper)

    fun typeToIdx(type: Class<T>): Int? =
        strToIdx[type.name] ?: run {
            var superclass = type.superclass
            while (superclass != null) {
                val idx = strToIdx[superclass.name]
                if (idx != null) {return@run idx}
                superclass = superclass.superclass
            }
            null
        }
    fun idxToSupplier(idx: Int) = typeToSupplier[idxToClass[idx]]
    fun idxToType(idx: Int) = idxToClass[idx]
    fun getSchema(): Map<String, Int> = strToIdx
    fun setSchema(schema: Map<Int, String>) {
        if (!schema.values.containsAll(strToIdx.keys)) {throw AssertionError("Schemas are incompatible")}
        val oldStrToIdx = strToIdx.toMap()
        val oldIdxToClass = idxToClass.toList()

        idxToClass.clear()
        strToIdx.clear()
        schema.forEach { (sIdx, sType) ->
            idxToClass[sIdx] = oldIdxToClass[oldStrToIdx[sType]!!]
            strToIdx[sType] = sIdx
        }

        val left = oldIdxToClass.toSet().minus(idxToClass.toSet())
        left.forEach {
            idxToClass.add(it)
            strToIdx[it.name] = idxToClass.size - 1
        }
    }

    fun asList() = suppliers.toList()
}

//TODO convert all Registry to ClassRegistry
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
    //TODO this will not work
    fun setSchema(schema: Map<Int, String>) {
        if (!schema.values.containsAll(strToIdx.keys)) { throw AssertionError("Schemas are incompatible") }
        schema.forEach {(k, v) -> strToIdx[v] = k}
    }
}