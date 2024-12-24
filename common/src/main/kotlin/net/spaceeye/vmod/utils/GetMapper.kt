package net.spaceeye.vmod.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.valkyrienskies.core.impl.util.serialization.*

fun getMapper(): ObjectMapper {
    return VSJacksonUtil.dtoMapper
}