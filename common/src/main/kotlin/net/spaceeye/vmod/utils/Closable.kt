package net.spaceeye.vmod.utils

private val serverClosable = mutableListOf<ServerClosable>()
private val clientClosable = mutableListOf<ClientClosable>()

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