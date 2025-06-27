package net.spaceeye.vmod.utils

import net.spaceeye.vmod.events.SessionEvents

inline fun onServerTick(crossinline fn: () -> Unit) {
    SessionEvents.serverOnTick.on { (server), unsubscribe ->
        unsubscribe.invoke()
        fn()
    }
}