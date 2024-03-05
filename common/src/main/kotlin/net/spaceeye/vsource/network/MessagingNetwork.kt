package net.spaceeye.vsource.network

import net.spaceeye.vsource.ELOG
import net.spaceeye.vsource.utils.ServerClosable

typealias handlerType = (msg: Message, unregister: () -> Unit) -> Unit

object MessagingNetwork: ServerClosable() {
    val listeners = mutableMapOf<String, MutableList<handlerType>>()

    fun register(channel: String, fn: handlerType) {
        listeners.getOrPut(channel) { mutableListOf() }.add(fn)
    }

    fun notify(channel: String, msg: Message) {
        val toRemove = mutableListOf<handlerType>()
        try {
            (listeners[channel] ?: return).forEach { handler: handlerType -> handler(msg) { toRemove.add(handler) } }
            listeners[channel]!!.removeAll(toRemove)
        } catch (e: Exception) {
            ELOG("NOTIFY HAS FAILED.\n${e.stackTraceToString()}")
        }
    }

    override fun close() {
        listeners.clear()
    }
}