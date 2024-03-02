package net.spaceeye.vsource.network

import net.spaceeye.vsource.utils.ServerClosable

typealias handlerType = (msg: Message, unregister: () -> Unit) -> Unit

object MessagingNetwork: ServerClosable() {
    val listeners = mutableMapOf<String, MutableList<handlerType>>()

    fun register(channel: String, fn: handlerType) {
        listeners.getOrPut(channel) { mutableListOf() }.add(fn)
    }

    fun notify(channel: String, msg: Message) {
        val toRemove = mutableListOf<handlerType>()
        (listeners[channel] ?: return).forEach { handler: handlerType -> handler(msg) {toRemove.add(handler)} }
        listeners[channel]!!.removeAll(toRemove)
    }

    override fun close() {
        listeners.clear()
    }
}