package net.spaceeye.vmod.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.valkyrienskies.core.impl.util.serialization.FastUtilModule
import org.valkyrienskies.core.impl.util.serialization.GuaveSerializationModule
import org.valkyrienskies.core.impl.util.serialization.JOMLSerializationModule
import org.valkyrienskies.core.impl.util.serialization.VSSerializationModule

fun getMapper(): ObjectMapper {
    val mapper = ObjectMapper()
    return mapper
        .registerModule(JOMLSerializationModule())
        .registerModule(VSSerializationModule())
        .registerModule(GuaveSerializationModule())
        .registerModule(FastUtilModule())
        .registerKotlinModule()
        .setVisibility(
            mapper.visibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.ANY)
                .withSetterVisibility(JsonAutoDetect.Visibility.ANY)
        ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}