package ru.l0sty.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.litote.kmongo.id.serialization.IdKotlinXSerializationModule

val json = Json { serializersModule = IdKotlinXSerializationModule; ignoreUnknownKeys = true; encodeDefaults = true }

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(json)
    }
}
