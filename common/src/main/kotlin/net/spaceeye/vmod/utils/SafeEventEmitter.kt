package net.spaceeye.vmod.utils

import java.util.concurrent.ConcurrentHashMap

class SafeEventEmitter<T> {
    val events = ConcurrentHashMap.newKeySet<(data: T, unsubscribe: () -> Unit) -> Unit>()

    fun on(fn: (data: T, unsubscribe: () -> Unit) -> Unit) {
        events.add(fn)
    }

    fun emit(data: T) = synchronized(events) {
        val toRemove = mutableSetOf<(data: T, unsubscribe: () -> Unit) -> Unit>()
        events.forEach { event ->
            var unsubscribe = false
            event.invoke(data) {unsubscribe = true}
            if (unsubscribe) {toRemove.add(event)}
        }
        events.removeAll(toRemove)
    }

    fun clear() {
        events.clear()
    }
}