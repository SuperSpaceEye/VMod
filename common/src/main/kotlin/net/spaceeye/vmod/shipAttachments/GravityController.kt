package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.vsStuff.VSGravityManager
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.assembly.ICopyableAttachment
import java.util.function.Supplier

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GravityController(
    var dimensionId: String,
): ShipPhysicsListener, ICopyableAttachment {
    var useDimensionGravity = true

    @JsonIgnore
    var dimensionGravity = VSGravityManager.getDimensionGravityMutableReference(dimensionId) ?: JVector3d(0.0, -10.0, 0.0)
//    @JsonSerialize(using = MyVectorSerializer::class)
//    @JsonDeserialize(using = MyVectorDeserializer::class)
    @JsonProperty(required = false)
    var gravityVector = dimensionGravity

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val gravityVector = if (useDimensionGravity) dimensionGravity else gravityVector
        val forceDiff = gravityVector.sub(dimensionGravity, JVector3d()).mul(physShip.mass)
        if (forceDiff.lengthSquared() < Float.MIN_VALUE) return

        //TODO
        physShip.applyInvariantForce(forceDiff)
    }

    fun reset() {
        gravityVector = VSGravityManager.getDimensionGravityMutableReference(dimensionId) ?: JVector3d(0.0, -10.0, 0.0)
        useDimensionGravity = true
    }

    @JsonIgnore
    fun effectiveGravity() = if (useDimensionGravity) dimensionGravity else gravityVector
    override fun onCopy(level: Supplier<ServerLevel>, shipOn: LoadedServerShip, shipsToBeSaved: List<ServerShip>, centerPositions: Map<ShipId, org.joml.Vector3dc>) {}
    override fun onPaste(level: Supplier<ServerLevel>, shipOn: LoadedServerShip, loadedShips: Map<Long, ServerShip>, centerPositions: Map<ShipId, Pair<org.joml.Vector3dc, org.joml.Vector3dc>>) {}

    companion object {
        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment(GravityController::class.java)
                ?: GravityController(ship.chunkClaimDimension).also {
                    ship.setAttachment(it)
                }
    }
}