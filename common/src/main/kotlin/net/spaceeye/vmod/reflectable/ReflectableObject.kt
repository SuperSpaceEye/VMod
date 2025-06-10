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
}


interface ReflectableObject {
    /**
     * If you can't directly serialize some class, make main class inherit ReflectableObject, and override this fn so that it returns state instance
     */
    @get:JsonIgnore val reflectObjectOverride: ReflectableObject? get() = null

    @JsonIgnore
    @ApiStatus.NonExtendable
    fun getAllReflectableItems(): List<ReflectableItemDelegate<*>> {
        reflectObjectOverride?.also { return it.getAllReflectableItems() }

        val toReturn = mutableListOf<ReflectableItemDelegate<*>>()

        val constructorItems: MutableSet<String> = mutableSetOf()
        if (this::class.isData) {
            val order = this::class.primaryConstructor?.parameters ?: listOf()
            val orderNames = order.map { it.name!! }
            val members = orderNames.map { item -> this::class.memberProperties.find { it.name == item }!! }
            val delegates = members.map { it -> ReflectableItemDelegate(-1, it.call(this)!!) }

            toReturn.addAll(delegates)
            constructorItems.addAll(orderNames)
        }

        val memberProperties = this::class.memberProperties.filter {
            !constructorItems.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !ReflectableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as ReflectableItemDelegate<*>

            delegate
        }.sortedBy { it.reflectionPos }

        toReturn.addAll(memberProperties)

        return toReturn
    }

    @JsonIgnore
    @ApiStatus.NonExtendable
    fun getReflectableItemsWithoutDataclassConstructorItems(): List<ReflectableItemDelegate<*>> {
        reflectObjectOverride?.also { return it.getAllReflectableItems() }

        val constructorItems = (if (this::class.isData) this::class.primaryConstructor?.parameters?.map { it.name }?.toSet() else null) ?: setOf()
        return this::class.memberProperties.filter {
            !constructorItems.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !ReflectableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as ReflectableItemDelegate<*>

            delegate
        }.sortedBy { it.reflectionPos }
    }
}

object ReflectableItem {
    @JvmStatic fun <T: Any> get(pos: Int, default: T): ReflectableItemDelegate<T> {
        return ReflectableItemDelegate(pos, default)
    }
}