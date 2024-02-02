package net.spaceeye.vsource.utils

import net.minecraft.core.BlockPos
import kotlin.math.max
import kotlin.math.min

typealias MCVector3d = com.mojang.math.Vector3d
typealias MCVector3f = com.mojang.math.Vector3f
typealias JVector3d  = org.joml.Vector3d
typealias JVector3dc = org.joml.Vector3dc
typealias MCVec3     = net.minecraft.world.phys.Vec3

class Vector3d(x:Number, y:Number, z:Number) {
    var x = x.toDouble()
    var y = y.toDouble()
    var z = z.toDouble()

    constructor(): this(0, 0, 0)
    constructor(o: Vector3d): this(o.x, o.y, o.z)
    constructor(o: JVector3d): this(o.x, o.y, o.z)
    constructor(o: MCVector3d): this(o.x, o.y, o.z)
    constructor(o: MCVector3f): this(o.x(), o.y(), o.z())
    constructor(o: JVector3dc): this(o.x(), o.y(), o.z())
    constructor(o: BlockPos): this(o.x, o.y, o.z)
    constructor(o: MCVec3): this(o.x, o.y, o.z)

    inline fun toD(x:Number, y: Number, z: Number): Array<Double> {return arrayOf(x.toDouble(), y.toDouble(), z.toDouble())}

    inline fun toMCVector3d(): MCVector3d {return MCVector3d(x, y, z) }
    inline fun toJomlVector3d(): JVector3d {return JVector3d(x, y, z) }
    inline fun toArray(): Array<Double> {return arrayOf(x, y, z) }
    inline fun toBlockPos(): BlockPos {return BlockPos(x, y, z) }
    inline fun toMCVec3(): MCVec3 {return MCVec3(x, y, z)}

    inline override fun toString(): String = "{${x} ${y} ${z}}"

    inline fun abs  (dest: Vector3d): Vector3d {dest.x = kotlin.math.abs(x)  ; dest.y = kotlin.math.abs(y)  ; dest.z = kotlin.math.abs(z)  ; return dest}
    inline fun floor(dest: Vector3d): Vector3d {dest.x = kotlin.math.floor(x); dest.y = kotlin.math.floor(y); dest.z = kotlin.math.floor(z); return dest}

    inline fun abs  (): Vector3d {return abs  (Vector3d())}
    inline fun floor(): Vector3d {return floor(Vector3d())}

    inline fun sabs  (): Vector3d {return abs  (this)}
    inline fun sfloor(): Vector3d {return floor(this)}

    inline fun floorCompare(other: Vector3d): Boolean {
        return     kotlin.math.floor(x) == kotlin.math.floor(other.x)
                && kotlin.math.floor(y) == kotlin.math.floor(other.y)
                && kotlin.math.floor(z) == kotlin.math.floor(other.z)
    }

    inline fun scross(x: Number, y: Number, z: Number): Vector3d {
        val (x,y,z) = toD(x,y,z)
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    inline fun cross(x: Number, y: Number, z: Number): Vector3d { return Vector3d(this).scross(x, y, z) }

    inline fun scross(v: Vector3d): Vector3d { return scross(v.x, v.y, v.z) }
    inline fun cross(v: Vector3d): Vector3d { return cross(v.x, v.y, v.z)}

    inline fun sqrDist(): Double {return x*x + y*y + z*z}
    inline fun dist(): Double {return Math.sqrt(x*x + y*y + z*z)}

    inline fun sign(): Vector3d {
        return Vector3d(
        if (x < 0) {-1.0} else {1.0},
        if (y < 0) {-1.0} else {1.0},
        if (z < 0) {-1.0} else {1.0})
    }

    inline fun normalize(length: Number, dest: Vector3d): Vector3d {
        val length = length.toDouble()
        val invLength = 1.0/Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        return dest
    }

    inline fun normalize(length: Number):Vector3d  { return normalize(length, Vector3d()) }
    inline fun snormalize(length: Number):Vector3d { return normalize(length, this) }

    inline fun normalize(): Vector3d {return normalize(1)}
    inline fun snormalize(): Vector3d {return snormalize(1)}
    inline fun normalize(dest: Vector3d): Vector3d {return normalize(1, dest)}

    inline fun sclamp(min_: Double, max_: Double): Vector3d {
        x = max(min(x, max_), min_)
        y = max(min(y, max_), min_)
        z = max(min(z, max_), min_)
        return this
    }

    inline fun dot(x: Double, y: Double, z: Double): Double {return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z)); }
    inline fun dot(other: Vector3d): Double {return dot(other.x, other.y, other.z)}

    inline fun add(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x + other.x
        dest.y = y + other.y
        dest.z = z + other.z
        return dest
    }

    inline fun sub(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x - other.x
        dest.y = y - other.y
        dest.z = z - other.z
        return dest
    }

    inline fun div(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x / other.x
        dest.y = y / other.y
        dest.z = z / other.z
        return dest
    }

    inline fun mul(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x * other.x
        dest.y = y * other.y
        dest.z = z * other.z
        return dest
    }

    inline fun rem(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x % other.x
        dest.y = y % other.y
        dest.z = z % other.z
        return dest
    }


    inline fun add(other: Double, dest: Vector3d): Vector3d {
        dest.x = x + other
        dest.y = y + other
        dest.z = z + other
        return dest
    }

    inline fun sub(other: Double, dest: Vector3d): Vector3d {
        dest.x = x - other
        dest.y = y - other
        dest.z = z - other
        return dest
    }

    inline fun div(other: Double, dest: Vector3d): Vector3d {
        dest.x = x / other
        dest.y = y / other
        dest.z = z / other
        return dest
    }

    inline fun mul(other: Double, dest: Vector3d): Vector3d {
        dest.x = x * other
        dest.y = y * other
        dest.z = z * other
        return dest
    }

    inline fun rem(other: Double, dest: Vector3d): Vector3d {
        dest.x = x % other
        dest.y = y % other
        dest.z = z % other
        return dest
    }

    inline fun sadd(x:Number, y: Number, z: Number): Vector3d {return add(Vector3d(x,y,z), this)}
    inline fun ssub(x:Number, y: Number, z: Number): Vector3d {return sub(Vector3d(x,y,z), this)}
    inline fun smul(x:Number, y: Number, z: Number): Vector3d {return mul(Vector3d(x,y,z), this)}
    inline fun sdiv(x:Number, y: Number, z: Number): Vector3d {return div(Vector3d(x,y,z), this)}
    inline fun srem(x:Number, y: Number, z: Number): Vector3d {return rem(Vector3d(x,y,z), this)}

    inline fun sadd(other:Number): Vector3d {return add(Vector3d(other,other,other), this)}
    inline fun ssub(other:Number): Vector3d {return sub(Vector3d(other,other,other), this)}
    inline fun smul(other:Number): Vector3d {return mul(Vector3d(other,other,other), this)}
    inline fun sdiv(other:Number): Vector3d {return div(Vector3d(other,other,other), this)}
    inline fun srem(other:Number): Vector3d {return rem(Vector3d(other,other,other), this)}

    inline fun add(x:Number, y: Number, z: Number): Vector3d {return add(Vector3d(x,y,z), Vector3d())}
    inline fun sub(x:Number, y: Number, z: Number): Vector3d {return sub(Vector3d(x,y,z), Vector3d())}
    inline fun mul(x:Number, y: Number, z: Number): Vector3d {return mul(Vector3d(x,y,z), Vector3d())}
    inline fun div(x:Number, y: Number, z: Number): Vector3d {return div(Vector3d(x,y,z), Vector3d())}
    inline fun rem(x:Number, y: Number, z: Number): Vector3d {return rem(Vector3d(x,y,z), Vector3d())}

    inline fun rdiv(other: Vector3d, dest: Vector3d): Vector3d {return other.div(this, dest)}
    inline fun rdiv(other: Double, dest: Vector3d):   Vector3d {return Vector3d(other, other, other).div(this, dest)}
    inline fun rdiv(x:Number, y: Number, z: Number):  Vector3d {return rdiv(Vector3d(x,y,z), Vector3d())}
    inline fun rdiv(other:Double): Vector3d {return rdiv(other, Vector3d())}

    inline fun srdiv(x:Number, y: Number, z: Number):  Vector3d {return rdiv(Vector3d(x,y,z), this)}
    inline fun srdiv(other:Double): Vector3d {return rdiv(other, this)}

    inline operator fun unaryPlus():  Vector3d {return this}
    inline operator fun unaryMinus(): Vector3d {return Vector3d(-x, -y, -z)}
    inline operator fun inc(): Vector3d {x++; y++; z++; return this}
    inline operator fun dec(): Vector3d {x--; y--; z--; return this}




    inline operator fun plus (other: Vector3d): Vector3d { return add(other, Vector3d()) }
    inline operator fun minus(other: Vector3d): Vector3d { return sub(other, Vector3d()) }
    inline operator fun times(other: Vector3d): Vector3d { return mul(other, Vector3d()) }
    inline operator fun div  (other: Vector3d): Vector3d { return div(other, Vector3d()) }
    inline operator fun rem  (other: Vector3d): Vector3d { return rem(other, Vector3d()) }
    inline operator fun plusAssign (other: Vector3d) {add(other, this)}
    inline operator fun minusAssign(other: Vector3d) {sub(other, this)}
    inline operator fun timesAssign(other: Vector3d) {mul(other, this)}
    inline operator fun divAssign  (other: Vector3d) {div(other, this)}
    inline operator fun remAssign  (other: Vector3d) {rem(other, this)}

    inline operator fun plus (other: Number): Vector3d { return add(other.toDouble(), Vector3d())}
    inline operator fun minus(other: Number): Vector3d { return sub(other.toDouble(), Vector3d())}
    inline operator fun times(other: Number): Vector3d { return mul(other.toDouble(), Vector3d())}
    inline operator fun div  (other: Number): Vector3d { return div(other.toDouble(), Vector3d())}
    inline operator fun rem  (other: Number): Vector3d { return rem(other.toDouble(), Vector3d())}
    inline operator fun plusAssign (other: Number) { add(other.toDouble(), this)}
    inline operator fun minusAssign(other: Number) { sub(other.toDouble(), this)}
    inline operator fun timesAssign(other: Number) { mul(other.toDouble(), this)}
    inline operator fun divAssign  (other: Number) { div(other.toDouble(), this)}
    inline operator fun remAssign  (other: Number) { rem(other.toDouble(), this)}
}