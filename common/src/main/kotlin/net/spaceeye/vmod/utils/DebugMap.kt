package net.spaceeye.vmod.utils

object DebugMap: MutableMap<Any, Any> {
    val theMap = mutableMapOf<Any, Any>()

    override val size: Int get() = theMap.size
    override val keys: MutableSet<Any> get() = theMap.keys
    override val values: MutableCollection<Any> get() = theMap.values
    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>> get() = theMap.entries

    override fun put(key: Any, value: Any): Any? = theMap.put(key, value)
    override fun remove(key: Any): Any? = theMap.remove(key)
    override fun putAll(from: Map<out Any, Any>) = theMap.putAll(from)
    override fun clear() = theMap.clear()
    override fun isEmpty(): Boolean = theMap.isEmpty()
    override fun containsKey(key: Any): Boolean = theMap.containsKey(key)
    override fun containsValue(value: Any): Boolean = theMap.containsValue(value)
    override fun get(key: Any): Any? = theMap.get(key)
}