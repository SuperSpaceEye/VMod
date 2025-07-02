package net.spaceeye.vmod.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.valkyrienskies.core.impl.util.serialization.*
import java.awt.Color

private class ColorSerializer(): JsonSerializer<Color>() {
    override fun handledType(): Class<Color> = Color::class.java
    override fun serialize(value: Color, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("r", value.red)
        gen.writeNumberField("g", value.green)
        gen.writeNumberField("b", value.blue)
        gen.writeEndObject()
    }
}

private class ColorDeserializer(): JsonDeserializer<Color>() {
    override fun handledType(): Class<*>? = Color::class.java
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Color? {
        val node = ctxt.readTree(p)
        return Color(
            node.get("r").numberValue().toInt(),
            node.get("g").numberValue().toInt(),
            node.get("b").numberValue().toInt(),
        )
    }
}

fun getMapper(): ObjectMapper {
    SimpleModule().addSerializer(ColorSerializer())

    return VSJacksonUtil.dtoMapper
        .copy()
        .registerModule(
            SimpleModule().addSerializer(ColorSerializer()).addDeserializer(Color::class.java, ColorDeserializer())
        )
}