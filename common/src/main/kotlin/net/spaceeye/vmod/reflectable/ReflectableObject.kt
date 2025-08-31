package net.spaceeye.vmod.reflectable

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

open class ReflectableItemDelegate <T : Any>(
    var reflectionPos: Int,
    var it: T?,
    val metadata: MutableMap<String, Any> = mutableMapOf<String, Any>(),
    var setWrapper: ((old: T, new: T) -> T)? = null,
    var getWrapper: ((value: T) -> T)? = null
) {
    lateinit var cachedName: String
    open operator fun getValue(thisRef: Any?, property: KProperty<*>?):T {
        return getWrapper?.invoke(it!!) ?: it!!
    }

    open operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: Any) {
        value as T
        it = setWrapper?.invoke(it!!, value) ?: value
    }

    open operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReflectableItemDelegate<T> {
        cachedName = property.name
        return this
    }

    override fun toString(): String {
        return "[Name: $cachedName | Item: $it | Pos: $reflectionPos | Set wrapper: ${setWrapper != null} | Get wrapper: ${getWrapper != null} | Metadata: $metadata]"
    }

    fun setSetWrapper(fn: (old: T, new:T) -> T): ReflectableItemDelegate<T> { setWrapper = fn; return this }
    fun setGetWrapper(fn: (value: T) -> T): ReflectableItemDelegate<T> { getWrapper = fn; return this }
}

//TODO add explanation
annotation class SubReflectable

interface ReflectableObject {
    /**
     * If you can't directly serialize some class, make main class inherit ReflectableObject, and override this fn so that it returns state instance
     */
    @get:JsonIgnore val reflectObjectOverride: ReflectableObject? get() = null

    private fun iterableResolver(items: Iterable<*>): List<ReflectableItemDelegate<*>> {
        return items.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            when (item) {
                is ReflectableObject -> item.getAllReflectableItems()
                is Iterable<*> -> iterableResolver(item)
                else -> throw AssertionError("Type ${item.javaClass} not supported")
            }
        }.flatten()
    }

    @JsonIgnore
    @ApiStatus.NonExtendable
    fun getAllReflectableItems(processSubReflectables: Boolean = false, filterBy: (ReflectableItemDelegate<*>) -> Boolean = {true}): List<ReflectableItemDelegate<*>> {
        reflectObjectOverride?.also { return it.getAllReflectableItems() }

        val toReturn = mutableListOf<ReflectableItemDelegate<*>>()

        if (this::class.isData) {
            val order = this::class.primaryConstructor?.parameters ?: listOf()
            val orderNames = order.map { it.name!! }
            val members = orderNames.map { item -> this::class.memberProperties.find { it.name == item }!! }
            val delegates = members
                .map { it -> ReflectableItemDelegate(-1, it.call(this)!!) }
                .filter(filterBy)

            toReturn.addAll(delegates)
        }
        toReturn.addAll(getReflectableItemsWithoutDataclassConstructorItems(processSubReflectables, filterBy))

        return toReturn
    }

    @JsonIgnore
    @ApiStatus.NonExtendable
    fun getReflectableItemsWithoutDataclassConstructorItems(processSubReflectables: Boolean = false, filterBy: (ReflectableItemDelegate<*>) -> Boolean = {true}): List<ReflectableItemDelegate<*>> {
        reflectObjectOverride?.also { return it.getAllReflectableItems() }

        val constructorItems = (if (this::class.isData) this::class.primaryConstructor?.parameters?.map { it.name }?.toSet() else null) ?: emptySet()

        val subReflectables = mutableListOf<Any>()

        val delegates = this::class.memberProperties.filter {
            !constructorItems.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField ?: return@mapNotNull null
            if (processSubReflectables && item.annotations.contains(SubReflectable())) {
                javaField.isAccessible = true
                val item = javaField.get(this) ?: return@mapNotNull null
                subReflectables.add(item)
                return@mapNotNull null
            }

            if (!ReflectableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            javaField.get(this) as ReflectableItemDelegate<*>
        }.sortedBy {
            it.reflectionPos
        }.filter(filterBy).toMutableList()

        delegates.addAll(subReflectables.map { item ->
            when (item) {
                is ReflectableObject -> item.getAllReflectableItems()
                is Iterable<*> -> iterableResolver(item)
                else -> throw AssertionError("Type ${item.javaClass} not supported")
            }
        }.flatten().filter(filterBy))

        return delegates
    }

    @JsonIgnore
    @ApiStatus.NonExtendable
    fun setFromVararg(items: Array<out Any?>) {
        val properties = getAllReflectableItems()
        items.forEachIndexed { i, arg ->
            if (arg == null) return@forEachIndexed
            val item = properties[i]
            if (item.it!!::class == arg::class) { item.setValue(null, null, arg) } else { throw AssertionError("vararg items do not match") }
        }
    }
}

object ReflectableItem {
    @JvmStatic fun <T: Any> get(pos: Int, default: T): ReflectableItemDelegate<T> {
        return ReflectableItemDelegate(pos, default)
    }
}