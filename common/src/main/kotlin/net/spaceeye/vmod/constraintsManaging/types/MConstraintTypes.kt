package net.spaceeye.vmod.constraintsManaging.types

import java.util.function.Supplier

//TODO this will not really work if I decide to make several addons to VMod because depending on
// loading order the shit will break
object MConstraintTypes {
    private val strToIdx = mutableMapOf<String, Int>()
    private val idxToStr = mutableMapOf<Int, String>()
    private val suppliers = mutableListOf<Supplier<MConstraint>>()

    init {
        register { BasicMConstraint() }
        register { RopeMConstraint() }
        register { WeldMConstraint() }
        register { HydraulicsMConstraint() }
        register { AxisMConstraint() }
    }

    private fun register(supplier: Supplier<MConstraint>) {
        suppliers.add(supplier)
        strToIdx[supplier.get().typeName] = suppliers.size - 1
        idxToStr[suppliers.size - 1] = supplier.get().typeName
    }

    fun typeToIdx(type: String) = strToIdx[type]
    fun idxToSupplier(idx: Int) = suppliers[idx]
    fun idxToType(idx: Int) = idxToStr[idx]
}