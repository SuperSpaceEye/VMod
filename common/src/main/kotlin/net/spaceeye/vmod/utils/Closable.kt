package net.spaceeye.vmod.utils

private val serverClosable = mutableListOf<Closable>()
private val clientClosable = mutableListOf<Closable>()

fun closeServerObjects() { serverClosable.forEach { it.close() } }
fun closeClientObjects() { clientClosable.forEach { it.close() } }

abstract class Closable {
    abstract fun close()
}

abstract class ServerClosable: Closable() {
    init {
        serverClosable.add(this)
    }
}

abstract class ClientClosable: Closable() {
    init {
        clientClosable.add(this)
    }
}

fun addCustomServer(fn: () -> Unit) {
    serverClosable.add(object : Closable() {
        override fun close() {
            fn()
        }
    })
}

fun addCustomClient(fn: () -> Unit) {
    clientClosable.add(object : Closable() {
        override fun close() {
            fn()
        }
    })
}