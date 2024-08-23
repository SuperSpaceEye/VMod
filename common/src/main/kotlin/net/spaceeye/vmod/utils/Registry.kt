package net.spaceeye.vmod.utils

import java.util.function.Supplier
import kotlin.reflect.KClass

//TODO revise?
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

    fun typeToSupplier(clazz: Class<T>)
        = typeToSupplier[clazz] ?: throw AssertionError("Type ${clazz.name} wasn't registered")

    fun register(clazz: KClass<*>) = register(clazz.java as Class<T>)
    fun typeToSupplier(clazz: KClass<*>) = typeToSupplier(clazz.java as Class<T>)
    inline fun <TT: Any> registerWrapper(clazz: KClass<TT>, crossinline wrapper: (item: TT) -> T) = registerWrapper(clazz.java as Class<T>) { item -> wrapper(item as TT) }

    fun typeToIdx(type: Class<*>): Int =
        strToIdx[getName(type)] ?: run {
            var superclass = type.superclass
            while (superclass != null) {
                val idx = strToIdx[getName(superclass)]
                if (idx != null) {return@run idx}
                superclass = superclass.superclass
            }
            null
        } ?: throw AssertionError("Class ${getName(type)} wasn't registered nor were registered any superclasses")
    fun idxToSupplier(idx: Int) = typeToSupplier[idxToClass[idx]]!!
    fun idxToType(idx: Int) = idxToClass[idx]

    fun typeToString (type: Class<*>): String = getName(type)
    fun strTypeToSupplier(strType: String): Supplier<T> { return typeToSupplier[idxToClass[strToIdx[strType] ?: throw AssertionError("Type $strType wasn't registered")]] ?: throw AssertionError("Type $strType wasn't registered") }

    fun getSchema(): Map<String, Int> = strToIdx
    fun setSchema(schema: Map<Int, String>) {
        if (!schema.values.containsAll(strToIdx.keys)) {throw AssertionError("Schemas are incompatible")}
        val oldStrToIdx = strToIdx.toMap()
        val oldIdxToClass = idxToClass.toList()

        val maxIdx = schema.keys.max()

        val tIdxToClass = mutableListOf<Class<T>?>()
        val tStrToIdx = mutableMapOf<String, Int>()

        tIdxToClass.clear()
        tIdxToClass.addAll(MutableList(maxIdx+1) {null})

        tStrToIdx.clear()
        schema.forEach { (sIdx, sType) ->
            tIdxToClass[sIdx] = oldIdxToClass[oldStrToIdx[sType]!!]
            tStrToIdx[sType] = sIdx
        }

        val left = oldIdxToClass.toSet().minus(tIdxToClass.toSet())
        left.forEach {
            tIdxToClass.add(it!!)
            tStrToIdx[getName(it)] = tIdxToClass.size - 1
        }

        idxToClass = tIdxToClass.map { it!! }.toMutableList()
        strToIdx = tStrToIdx
    }

    fun asList() = suppliers.toList()
    fun asTypesList() = idxToClass.toList()
}