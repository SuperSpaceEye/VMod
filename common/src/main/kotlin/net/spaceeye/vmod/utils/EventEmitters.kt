package net.spaceeye.vmod.utils

import java.util.concurrent.ConcurrentHashMap

class CancellableEventEmitter<Data> {
    val events = mutableSetOf<(data: Data, unsubscribe: () -> Unit) -> Boolean>()

    fun on(fn: (data: Data, unsubscribe: () -> Unit) -> Boolean) {
        events.add(fn)
    }

    fun emit(data: Data): Boolean = synchronized(events) {
        val toRemove = mutableSetOf<(data: Data, unsubscribe: () -> Unit) -> Boolean>()
        var cancel = false
        events.forEach { event ->
            var unsubscribe = false
            if (event.invoke(data) {unsubscribe = true}) {cancel = true}
            if (unsubscribe) {toRemove.add(event)}
        }
        events.removeAll(toRemove)
        return@synchronized cancel
    }

    fun clear() {
        events.clear()
    }
}

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