package net.spaceeye.vmod.limits

class ClientLimitsInstance {
    //TODO add client limits
}

class ClientLimits {
    var instance: ClientLimitsInstance = ClientLimitsInstance()

    fun update() { instance = ClientLimitsInstance() }
}