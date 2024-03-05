package net.spaceeye.vsource.limits

import kotlin.math.max
import kotlin.math.min

data class DoubleLimit(var minValue: Double = -Double.MAX_VALUE, var maxValue: Double = Double.MAX_VALUE) { fun get(num: Double) = max(minValue, min(maxValue, num)) }
data class IntLimit   (var minValue: Int    =  Int.MIN_VALUE,    var maxValue: Int    = Int.MAX_VALUE   ) { fun get(num: Int)    = max(minValue, min(maxValue, num)) }

class ServerLimitsInstance {
    val compliance = DoubleLimit(1e-300, 1.0)
    val maxForce = DoubleLimit(1.0)
    val fixedDistance = DoubleLimit(0.001)
    val extensionDistance = DoubleLimit(0.001)
    val extensionSpeed = DoubleLimit(0.001)
}

object ServerLimits {
    var instance: ServerLimitsInstance = ServerLimitsInstance()

    fun update() { instance = ServerLimitsInstance() }
}