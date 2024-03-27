package net.spaceeye.vmod.limits

class ClientLimitsInstance {
    //TODO implement this
    val renderingArea = DoubleLimit()
}

class ClientLimits {
    var instance: ClientLimitsInstance = ClientLimitsInstance()

    fun update() { instance = ClientLimitsInstance() }
}