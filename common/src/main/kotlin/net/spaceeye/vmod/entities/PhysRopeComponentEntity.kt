package net.spaceeye.vmod.entities

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityInLevelCallback
import net.spaceeye.vmod.VMEntities
import net.spaceeye.vmod.utils.vs.createD
import net.spaceeye.vmod.utils.vs.transformF
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.bodies.properties.BodyTransform
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.ships.properties.ShipInertiaData
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.apigame.physics.*
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.game.ships.*
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.mixin.accessors.entity.EntityAccessor
import kotlin.math.PI

class PhysRopeComponentEntity(type: EntityType<PhysRopeComponentEntity>, level: Level): Entity(type, level) {
    // Physics data, persistent
    var physicsEntityData: PhysicsEntityData? = null
        private set

    // The physics entity, transient, only exists server side after this entity has been added to a world
    var physicsEntityServer: PhysicsEntityServer? = null
        private set

    private var lerpPos: Vector3dc? = null
    private var lerpSteps = 0

    val radius: Double? get() = physicsEntityData?.let { (it.collisionShapeData as VSCapsuleCollisionShapeData).radius }
    val length: Double? get() = physicsEntityData?.let { (it.collisionShapeData as VSCapsuleCollisionShapeData).length }
    val mass:   Double? get() = physicsEntityData?.inertiaData?.mass

    init {
        if (level.isClientSide) {
//            ClientPhysEntitiesHolder.entityLoaded(id, this)
        }
    }

    fun setPhysicsEntityData(physicsEntityData: PhysicsEntityData) {
        this.physicsEntityData = physicsEntityData
        if (!this.level.isClientSide) {
            this.entityData.set(SHIP_ID_DATA, physicsEntityData.shipId.toString())
        }
    }

    override fun tick() {
        if (lerpPos == null) { lerpPos = position().toJOML() }
        if (level.isClientSide) { tickLerp(); super.tick(); return}

        val physicsEntityServerCopy = physicsEntityServer
        if (physicsEntityServerCopy != null) {
            val transform = physicsEntityServerCopy.shipTransform
            this.setPos(transform.positionInWorld.toMinecraft())
            this.physicsEntityData!!.transform = transform
        }
        this.tryCheckInsideBlocks()
    }

    override fun lerpTo(d: Double, e: Double, f: Double, g: Float, h: Float, i: Int, bl: Boolean) {
        this.lerpPos = Vector3d(d, e, f)
        this.lerpSteps = CLIENT_INTERP_STEPS
    }

    private fun tickLerp() {
        if (this.lerpSteps <= 0) { return }
        if (this.lerpSteps == 1) {
            setPos(lerpPos!!.x(), lerpPos!!.y(), lerpPos!!.z())
            lerpSteps = 0
            return
        }

        val d: Double = this.x + (this.lerpPos!!.x() - this.x) / this.lerpSteps.toDouble()
        val e: Double = this.y + (this.lerpPos!!.y() - this.y) / this.lerpSteps.toDouble()
        val f: Double = this.z + (this.lerpPos!!.z() - this.z) / this.lerpSteps.toDouble()

        --this.lerpSteps
        this.setPos(d, e, f)
    }

    fun getRenderTransform(shipObjectClientWorld: ShipObjectClientWorld): ShipTransform? {
        val shipIdString = entityData.get(SHIP_ID_DATA)
        if (shipIdString == "") {
            return null
        }
        val shipIdLong = shipIdString.toLong()
        val physEntityClient = shipObjectClientWorld.physicsEntities[shipIdLong]
        return physEntityClient?.renderTransform
    }

    override fun defineSynchedData() {
        entityData.define(SHIP_ID_DATA, "")
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun getAddEntityPacket(): Packet<*> {
        return ClientboundAddEntityPacket(this)
    }

    override fun saveWithoutId(compoundTag: CompoundTag): CompoundTag {
        val physicsEntityDataAsBytes = getMapper().writeValueAsBytes(physicsEntityData)
        compoundTag.putByteArray(PHYS_DATA_NBT_KEY, physicsEntityDataAsBytes)
        return super.saveWithoutId(compoundTag)
    }

    // Used when teleporting through nether portals to create a new entity that's almost the same as this one
    // Note how a new shipId is generated, since this is meant to be a new copy not the exact same one
    private fun loadForTeleport(compoundTag: CompoundTag) {
        if (!this.level.isClientSide && physicsEntityData != null) {
            throw IllegalStateException("This entity is already loaded!")
        }
        val physicsEntityDataAsBytes: ByteArray = compoundTag.getByteArray(PHYS_DATA_NBT_KEY)
        val oldPhysicsEntityData = getMapper().readValue<PhysicsEntityData>(physicsEntityDataAsBytes)
        val newShipId = TODO("FIX THIS") // TODO FIX THIS
//        val newShipId = (level.shipObjectWorld as ShipObjectServerWorld).allocateShipId(level.dimensionId)
        val newPhysicsEntityData = oldPhysicsEntityData.copyPhysicsEntityDataWithNewId(newShipId)
        // Change the shipId to be something new
        setPhysicsEntityData(newPhysicsEntityData)
        super.load(compoundTag)
    }

    override fun load(compoundTag: CompoundTag) {
        if (!this.level.isClientSide && physicsEntityData != null) {
            throw IllegalStateException("This entity is already loaded!")
        }
        val physicsEntityDataAsBytes: ByteArray = compoundTag.getByteArray(PHYS_DATA_NBT_KEY)
        val physicsEntityData = getMapper().readValue<PhysicsEntityData>(physicsEntityDataAsBytes)
        setPhysicsEntityData(physicsEntityData)
        super.load(compoundTag)

        if (!level.isClientSide) {
//            ServerPhysEntitiesHolder.entityLoaded(uuid, this)
        }
    }

    override fun setLevelCallback(callback: EntityInLevelCallback?) {
        super.setLevelCallback(callback)
        if (!this.level.isClientSide) {
            val isNull = (callback == null) || callback == EntityInLevelCallback.NULL
            if (!isNull) {
                // Try adding the rigid body of this entity from the world
                if (physicsEntityServer != null) {
                    throw IllegalStateException("Rigid body is already in the world!")
                }
                physicsEntityServer = (level.shipObjectWorld as ServerShipWorldCore).createPhysicsEntity(
                        physicsEntityData!!, level.dimensionId
                )
            } else {
                // Try removing the rigid body of this entity from the world
                if (physicsEntityServer == null) {
                    return
                    // throw IllegalStateException("Rigid body does not exist in the world!")
                }
                (level.shipObjectWorld as ServerShipWorldCore).deletePhysicsEntity(physicsEntityData!!.shipId)
                physicsEntityServer = null
            }
        }
    }

    override fun shouldRender(x: Double, y: Double, z: Double): Boolean {
        return false
    }

    override fun shouldRenderAtSqrDistance(d: Double): Boolean {
        return false
    }

    override fun moveTo(d: Double, e: Double, f: Double, g: Float, h: Float) {
        super.moveTo(d, e, f, g, h)
        if (!this.level.isClientSide) {
            val physicsEntityServerCopy = physicsEntityServer
            if (physicsEntityServerCopy != null) {
                val newPos = Vector3d(d, e, f)
                val teleportData = ShipTeleportDataImpl(newPos = newPos)
                level.shipObjectWorld
                //TODO FIX THIS
//                (this.level.shipObjectWorld as ShipObjectServerWorld).teleportPhysicsEntity(this.physicsEntityServer!!, teleportData)
            } else {
                physicsEntityData!!.transform = ShipTransformImpl.create(
                        Vector3d(d, e, f),
                        Vector3d(),
                        physicsEntityData!!.transform.shipToWorldRotation,
                )
            }
        }
    }

    // Used when teleporting through nether portals to create a new entity that's almost the same as this one
    override fun restoreFrom(entity: Entity) {
        val compoundTag = entity.saveWithoutId(CompoundTag())
        compoundTag.remove("Dimension")
        loadForTeleport(compoundTag)
        ((this as EntityAccessor).portalCooldown) = (entity as EntityAccessor).portalCooldown
        portalEntrancePos = entity.portalEntrancePos
    }

    override fun kill() {
//        ServerPhysEntitiesHolder.serverRemovedEntity.emit(ServerPhysEntitiesHolder.ServerRemovedEntity(uuid, this))
        super.kill()
    }

    override fun onClientRemoval() {
//        ClientPhysEntitiesHolder.clientRemovedEntity.emit(ClientPhysEntitiesHolder.ClientRemovedEntity(id, this))
        super.onClientRemoval()
    }

    companion object {
        private const val PHYS_DATA_NBT_KEY = "phys_entity_data"
        private const val CLIENT_INTERP_STEPS = 3

        // Use string because there is no LONG serializer by default SMH my head!
        private val SHIP_ID_DATA: EntityDataAccessor<String> =
                SynchedEntityData.defineId(PhysRopeComponentEntity::class.java, EntityDataSerializers.STRING)

        private fun getMapper(): ObjectMapper {
            return VSJacksonUtil.defaultMapper
        }

        @OptIn(VsBeta::class)
        fun createEntity(level: ServerLevel, mass: Double, radius: Double, length: Double, pos: Vector3d, rotation: Quaterniond, spawnStatic: Boolean = false): PhysRopeComponentEntity {
            val entity = VMEntities.PHYS_ROPE_COMPONENT.get().create(level)!!
            val shipId = level.shipObjectWorld.allocateShipId(level.dimensionId)

            val transform = transformF.createD(pos, rotation)
            val physEntityData = createCapsuleData(shipId, transform, radius, length, mass, spawnStatic)

            entity.setPhysicsEntityData(physEntityData)
            entity.setPos(pos.x, pos.y, pos.z)
            level.addFreshEntity(entity)
            if (!level.isClientSide) {
//                ServerPhysEntitiesHolder.entityLoaded(entity.uuid, entity)
            }
            return entity
        }

        //https://www.gamedev.net/tutorials/programming/math-and-physics/capsule-inertia-tensor-r3856/
        fun createCapsuleData(shipId: ShipId, transform: BodyTransform, radius: Double, length: Double, mass: Double, spawnStatic: Boolean=false): PhysicsEntityData {
            val h = length
            val r = radius
            val rSq = r * r

            val cM_temp = h * rSq * PI
            val hsM_temp = 2.0 * rSq * r * PI * (1.0/3.0)

            val density = mass / (cM_temp + hsM_temp)

            val cM = cM_temp * density
            val hsM = hsM_temp * density

            val inertiaTensor = Matrix3d()

            inertiaTensor.m11 = rSq * cM * 0.5

            inertiaTensor.m22 = inertiaTensor.m11 * 0.5 + cM * h * h * (1.0/12.0)
            inertiaTensor.m00 = inertiaTensor.m22

            val temp0 = hsM * 2.0 * rSq / 5.0
            inertiaTensor.m11 += temp0 * 2.0
            val temp1 = h * 0.5
            val temp2 = temp0 + hsM * (temp1 * temp1 + 3.0 * (1.0/8.0) * h * r)
            inertiaTensor.m00 += temp2 * 2.0
            inertiaTensor.m22 += temp2 * 2.0

            val inertiaData: ShipInertiaData = ShipInertiaDataImpl(Vector3d(), mass, inertiaTensor)
            return TODO()
//            return PhysicsEntityData(
//                    shipId,
//                    transform,
//                    inertiaData,
//                    Vector3d(),
//                    Vector3d(),
//                    VSCapsuleCollisionShapeData(radius, length),
//                    isStatic = spawnStatic
//            )
        }
    }
}