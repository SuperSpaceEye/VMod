package net.spaceeye.vmod.config

import kotlin.reflect.KProperty

open class BaseConfigDelegate <T : Any>(var it:T, var range: Pair<T, T>? = null, var description: String="No comment", val do_show:Boolean = true) {
    private lateinit var delegateRegister: DelegateRegisterItem

    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return ConfigDelegateRegister.getResolved(delegateRegister.resolved_name)() as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        ConfigDelegateRegister.setResolved(delegateRegister.resolved_name, value)
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseConfigDelegate<T> {
        delegateRegister = DelegateRegisterItem(thisRef, property, description, range, do_show = do_show)
        ConfigDelegateRegister.newEntry(delegateRegister, it)
        return this
    }
}

class CInt   (it:Int,     description: String="No comment", range: Pair<Int, Int>?       = Pair(Int.MIN_VALUE,    Int.MAX_VALUE),    do_show: Boolean=true): BaseConfigDelegate<Int>(it, range, description, do_show)
class CLong  (it:Long,    description: String="No comment", range: Pair<Long, Long>?     = Pair(Long.MIN_VALUE,   Long.MAX_VALUE),   do_show: Boolean=true): BaseConfigDelegate<Long>(it, range, description, do_show)
class CDouble(it:Double,  description: String="No comment", range: Pair<Double, Double>? = Pair(Double.MIN_VALUE, Double.MAX_VALUE), do_show: Boolean=true): BaseConfigDelegate<Double>(it, range, description, do_show)
class CBool  (it:Boolean, description: String="No comment", do_show: Boolean=true): BaseConfigDelegate<Boolean>(it, null, description, do_show)
class CString(it:String,  description: String="No comment", do_show: Boolean=true): BaseConfigDelegate<String>(it, null, description, do_show)