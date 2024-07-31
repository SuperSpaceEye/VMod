package net.spaceeye.vmod.utils

import java.util.function.Supplier

interface RegistryObject {
    val typeName: String
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

    fun typeToIdx(type: String) = strToIdx[type]
    fun typeToSupplier(type: String) = suppliers[strToIdx[type]!!]

    fun idxToSupplier(idx: Int) = suppliers[idx]

    fun idxToSupplier(idx: Int, schema: Map<Int, String>): Supplier<T>? {
        val name = schema[idx] ?: throw AssertionError("idx doesn't exist in schema")
        val currentIdx = strToIdx[name] ?: return null
        return suppliers[currentIdx]
    }

    fun getSchema(): Map<String, Int> = strToIdx
    fun setSchema(schema: Map<Int, String>) {
        if (!schema.values.containsAll(strToIdx.keys)) { throw AssertionError("Schemas are incompatible ") }
        schema.forEach {(k, v) -> strToIdx[v] = k}
    }

    fun asList() = suppliers.toList()
}