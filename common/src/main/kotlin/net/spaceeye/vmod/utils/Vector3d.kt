package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import kotlin.math.max
import kotlin.math.min

typealias JVector3d  = org.joml.Vector3d
typealias JVector3f  = org.joml.Vector3f
typealias JVector3dc = org.joml.Vector3dc
typealias JVector3i  = org.joml.Vector3i
typealias MCVec3     = net.minecraft.world.phys.Vec3

fun FriendlyByteBuf.writeVector3d(vector3d: Vector3d) {
    this.writeDouble(vector3d.x)
    this.writeDouble(vector3d.y)
    this.writeDouble(vector3d.z)
}

fun FriendlyByteBuf.readVector3d(): Vector3d {
    return Vector3d(
        this.readDouble(),
        this.readDouble(),
        this.readDouble()
    )
}

class Vector3d(x:Number, y:Number, z:Number) {
    @JvmField var x = x.toDouble()
    @JvmField var y = y.toDouble()
    @JvmField var z = z.toDouble()

    constructor(): this(0, 0, 0)
    constructor(o: Vector3d): this(o.x, o.y, o.z)
    constructor(o: JVector3d): this(o.x, o.y, o.z)
    constructor(o: JVector3f): this(o.x, o.y, o.z)
    constructor(o: JVector3dc): this(o.x(), o.y(), o.z())
    constructor(o: BlockPos): this(o.x, o.y, o.z)
    constructor(o: MCVec3): this(o.x, o.y, o.z)

    fun toD(x:Number, y: Number, z: Number): Array<Double> {return arrayOf(x.toDouble(), y.toDouble(), z.toDouble())}

    fun toJomlVector3d(): JVector3d {return JVector3d(x, y, z) }
    fun toJomlVector3i(): JVector3i {return JVector3i(x.toInt(), y.toInt(), z.toInt()) }
    fun toArray(): Array<Double> {return arrayOf(x, y, z) }
    fun toBlockPos(): BlockPos {return BlockPos(x.toInt(), y.toInt(), z.toInt()) }
    fun toMCVec3(): MCVec3 {return MCVec3(x, y, z)}

    fun copy() = Vector3d(x, y, z)

    override fun toString(): String = "{${x} ${y} ${z}}"

    fun abs  (dest: Vector3d): Vector3d {dest.x = kotlin.math.abs(x)  ; dest.y = kotlin.math.abs(y)  ; dest.z = kotlin.math.abs(z)  ; return dest}
    fun floor(dest: Vector3d): Vector3d {dest.x = kotlin.math.floor(x); dest.y = kotlin.math.floor(y); dest.z = kotlin.math.floor(z); return dest}

    fun abs  (): Vector3d {return abs  (Vector3d())}
    fun floor(): Vector3d {return floor(Vector3d())}

    fun sabs  (): Vector3d {return abs  (this)}
    fun sfloor(): Vector3d {return floor(this)}

    fun set(x: Double, y: Double, z: Double) {this.x = x; this.y = y; this.z = z}
    fun set(x: Number, y: Number, z: Number) {this.x = x.toDouble(); this.y = y.toDouble(); this.z = z.toDouble()}

    fun floorCompare(other: Vector3d): Boolean {
        return     kotlin.math.floor(x) == kotlin.math.floor(other.x)
                && kotlin.math.floor(y) == kotlin.math.floor(other.y)
                && kotlin.math.floor(z) == kotlin.math.floor(other.z)
    }

    fun scross(x: Number, y: Number, z: Number): Vector3d {
        val (x,y,z) = toD(x,y,z)
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun cross(x: Number, y: Number, z: Number): Vector3d { return Vector3d(this).scross(x, y, z) }

    fun scross(v: Vector3d): Vector3d { return scross(v.x, v.y, v.z) }
    fun cross(v: Vector3d): Vector3d { return cross(v.x, v.y, v.z)}

    fun sqrDist(): Double {return x*x + y*y + z*z}
    fun dist(): Double {return Math.sqrt(x*x + y*y + z*z)}

    fun sign(): Vector3d {
        return Vector3d(
        if (x < 0) {-1.0} else {1.0},
        if (y < 0) {-1.0} else {1.0},
        if (z < 0) {-1.0} else {1.0})
    }

    fun normalize(length: Number, dest: Vector3d): Vector3d {
        val length = length.toDouble()
        val invLength = 1.0/Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        return dest
    }

    fun normalize(length: Number):Vector3d  { return normalize(length, Vector3d()) }
    fun snormalize(length: Number):Vector3d { return normalize(length, this) }

    fun normalize(): Vector3d {return normalize(1)}
    fun snormalize(): Vector3d {return snormalize(1)}
    fun normalize(dest: Vector3d): Vector3d {return normalize(1, dest)}

    fun sclamp(min_: Double, max_: Double): Vector3d {
        x = max(min(x, max_), min_)
        y = max(min(y, max_), min_)
        z = max(min(z, max_), min_)
        return this
    }

    fun dot(x: Double, y: Double, z: Double): Double {return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z)); }
    fun dot(other: Vector3d): Double {return dot(other.x, other.y, other.z)}

    fun avg(): Double { return (x + y + z)/3.0 }

    fun add(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x + other.x
        dest.y = y + other.y
        dest.z = z + other.z
        return dest
    }

    fun sub(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x - other.x
        dest.y = y - other.y
        dest.z = z - other.z
        return dest
    }

    fun div(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x / other.x
        dest.y = y / other.y
        dest.z = z / other.z
        return dest
    }

    fun mul(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x * other.x
        dest.y = y * other.y
        dest.z = z * other.z
        return dest
    }

    fun rem(other: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x % other.x
        dest.y = y % other.y
        dest.z = z % other.z
        return dest
    }


    fun add(other: Double, dest: Vector3d): Vector3d {
        dest.x = x + other
        dest.y = y + other
        dest.z = z + other
        return dest
    }

    fun sub(other: Double, dest: Vector3d): Vector3d {
        dest.x = x - other
        dest.y = y - other
        dest.z = z - other
        return dest
    }

    fun div(other: Double, dest: Vector3d): Vector3d {
        dest.x = x / other
        dest.y = y / other
        dest.z = z / other
        return dest
    }

    fun mul(other: Double, dest: Vector3d): Vector3d {
        dest.x = x * other
        dest.y = y * other
        dest.z = z * other
        return dest
    }

    fun rem(other: Double, dest: Vector3d): Vector3d {
        dest.x = x % other
        dest.y = y % other
        dest.z = z % other
        return dest
    }

    fun sadd(x:Number, y: Number, z: Number): Vector3d {return add(Vector3d(x,y,z), this)}
    fun ssub(x:Number, y: Number, z: Number): Vector3d {return sub(Vector3d(x,y,z), this)}
    fun smul(x:Number, y: Number, z: Number): Vector3d {return mul(Vector3d(x,y,z), this)}
    fun sdiv(x:Number, y: Number, z: Number): Vector3d {return div(Vector3d(x,y,z), this)}
    fun srem(x:Number, y: Number, z: Number): Vector3d {return rem(Vector3d(x,y,z), this)}

    fun sadd(other:Number): Vector3d {return add(Vector3d(other,other,other), this)}
    fun ssub(other:Number): Vector3d {return sub(Vector3d(other,other,other), this)}
    fun smul(other:Number): Vector3d {return mul(Vector3d(other,other,other), this)}
    fun sdiv(other:Number): Vector3d {return div(Vector3d(other,other,other), this)}
    fun srem(other:Number): Vector3d {return rem(Vector3d(other,other,other), this)}

    fun add(x:Number, y: Number, z: Number): Vector3d {return add(Vector3d(x,y,z), Vector3d())}
    fun sub(x:Number, y: Number, z: Number): Vector3d {return sub(Vector3d(x,y,z), Vector3d())}
    fun mul(x:Number, y: Number, z: Number): Vector3d {return mul(Vector3d(x,y,z), Vector3d())}
    fun div(x:Number, y: Number, z: Number): Vector3d {return div(Vector3d(x,y,z), Vector3d())}
    fun rem(x:Number, y: Number, z: Number): Vector3d {return rem(Vector3d(x,y,z), Vector3d())}

    fun rdiv(other: Vector3d, dest: Vector3d): Vector3d {return other.div(this, dest)}
    fun rdiv(other: Double, dest: Vector3d):   Vector3d {return Vector3d(other, other, other).div(this, dest)}
    fun rdiv(x:Number, y: Number, z: Number):  Vector3d {return rdiv(Vector3d(x,y,z), Vector3d())}
    fun rdiv(other:Double): Vector3d {return rdiv(other, Vector3d())}

    fun srdiv(x:Number, y: Number, z: Number):  Vector3d {return rdiv(Vector3d(x,y,z), this)}
    fun srdiv(other:Double): Vector3d {return rdiv(other, this)}

    operator fun unaryPlus():  Vector3d {return this}
    operator fun unaryMinus(): Vector3d {return Vector3d(-x, -y, -z)}
    operator fun inc(): Vector3d {x++; y++; z++; return this}
    operator fun dec(): Vector3d {x--; y--; z--; return this}




    operator fun plus (other: Vector3d): Vector3d { return add(other, Vector3d()) }
    operator fun minus(other: Vector3d): Vector3d { return sub(other, Vector3d()) }
    operator fun times(other: Vector3d): Vector3d { return mul(other, Vector3d()) }
    operator fun div  (other: Vector3d): Vector3d { return div(other, Vector3d()) }
    operator fun rem  (other: Vector3d): Vector3d { return rem(other, Vector3d()) }
    operator fun plusAssign (other: Vector3d) {add(other, this)}
    operator fun minusAssign(other: Vector3d) {sub(other, this)}
    operator fun timesAssign(other: Vector3d) {mul(other, this)}
    operator fun divAssign  (other: Vector3d) {div(other, this)}
    operator fun remAssign  (other: Vector3d) {rem(other, this)}

    operator fun plus (other: Number): Vector3d { return add(other.toDouble(), Vector3d())}
    operator fun minus(other: Number): Vector3d { return sub(other.toDouble(), Vector3d())}
    operator fun times(other: Number): Vector3d { return mul(other.toDouble(), Vector3d())}
    operator fun div  (other: Number): Vector3d { return div(other.toDouble(), Vector3d())}
    operator fun rem  (other: Number): Vector3d { return rem(other.toDouble(), Vector3d())}
    operator fun plusAssign (other: Number) { add(other.toDouble(), this)}
    operator fun minusAssign(other: Number) { sub(other.toDouble(), this)}
    operator fun timesAssign(other: Number) { mul(other.toDouble(), this)}
    operator fun divAssign  (other: Number) { div(other.toDouble(), this)}
    operator fun remAssign  (other: Number) { rem(other.toDouble(), this)}
}