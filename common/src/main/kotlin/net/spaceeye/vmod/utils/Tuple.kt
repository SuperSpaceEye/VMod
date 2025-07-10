package net.spaceeye.vmod.utils

object Tuple {
    @JvmStatic fun <T1, T2>                         of(i1: T1, i2: T2) = Tuple2(i1, i2)
    @JvmStatic fun <T1, T2, T3>                     of(i1: T1, i2: T2, i3: T3) = Tuple3(i1, i2, i3)
    @JvmStatic fun <T1, T2, T3, T4>                 of(i1: T1, i2: T2, i3: T3, i4: T4) = Tuple4(i1, i2, i3, i4)
    @JvmStatic fun <T1, T2, T3, T4, T5>             of(i1: T1, i2: T2, i3: T3, i4: T4, i5: T5) = Tuple5(i1, i2, i3, i4, i5)
    @JvmStatic fun <T1, T2, T3, T4, T5, T6>         of(i1: T1, i2: T2, i3: T3, i4: T4, i5: T5, i6: T6) = Tuple6(i1, i2, i3, i4, i5, i6)
    @JvmStatic fun <T1, T2, T3, T4, T5, T6, T7>     of(i1: T1, i2: T2, i3: T3, i4: T4, i5: T5, i6: T6, i7: T7) = Tuple7(i1, i2, i3, i4, i5, i6, i7)
    @JvmStatic fun <T1, T2, T3, T4, T5, T6, T7, T8> of(i1: T1, i2: T2, i3: T3, i4: T4, i5: T5, i6: T6, i7: T7, i8: T8) = Tuple8(i1, i2, i3, i4, i5, i6, i7, i8)
}

data class Tuple2<T1, T2>(var i1: T1, var i2: T2)
data class Tuple3<T1, T2, T3>(var i1: T1, var i2: T2, var i3: T3)
data class Tuple4<T1, T2, T3, T4>(var i1: T1, var i2: T2, var i3: T3, var i4: T4)
data class Tuple5<T1, T2, T3, T4, T5>(var i1: T1, var i2: T2, var i3: T3, var i4: T4, var i5: T5)
data class Tuple6<T1, T2, T3, T4, T5, T6>(var i1: T1, var i2: T2, var i3: T3, var i4: T4, var i5: T5, var i6: T6)
data class Tuple7<T1, T2, T3, T4, T5, T6, T7>(var i1: T1, var i2: T2, var i3: T3, var i4: T4, var i5: T5, var i6: T6, val i7: T7)
data class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>(var i1: T1, var i2: T2, var i3: T3, var i4: T4, var i5: T5, var i6: T6, val i7: T7, val i8: T8)