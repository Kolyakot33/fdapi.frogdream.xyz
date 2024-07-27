package ru.l0sty.plugins

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.server.sessions.*

class RedisSessionStorage(private val prefix: String) : SessionStorage {
    val client: KredsClient = newClient(Endpoint.from("localhost:6379"))

    override suspend fun invalidate(id: String) {
        client.del("$prefix:$id")
    }

    override suspend fun read(id: String): String {
        println("Read ${id}")
        return client.get("$prefix:$id") ?: throw NoSuchElementException("No value found for $id")
    }

    override suspend fun write(id: String, value: String) {
        client.set("$prefix:$id", value)
    }

}