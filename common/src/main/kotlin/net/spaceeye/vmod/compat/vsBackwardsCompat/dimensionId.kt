package org.valkyrienskies.mod.api

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.mod.common.dimensionId

val ServerLevel.dimensionId get() = this.dimensionId