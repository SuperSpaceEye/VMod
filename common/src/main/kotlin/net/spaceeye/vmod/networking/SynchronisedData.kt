package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager.Side

abstract class SynchronisedData (
    streamName: String,
    transmitterSide: Side,
    currentSide: Side,
    partByteMaxAmount: Int = 1000000
) {
    
}