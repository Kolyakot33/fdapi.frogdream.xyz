package ru.l0sty.plugins

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.viascom.nanoid.NanoId
import java.time.Duration

object RedisTokenStorage {
    val client: KredsClient = newClient(Endpoint.from("localhost:6379"))
    val ttl = Duration.ofDays(14).toSeconds().toULong()
    val prefix = "fdapi:"

    suspend fun getIdByToken(token: String): String? {
        client.get(prefix + token)?.let {
            client.expire(prefix + token, ttl)
            return it
        } ?: throw NoSuchElementException("No value found for $token")
    }

    suspend fun createToken(id: String): String {
        val token = NanoId.generate(48)
        client.set(prefix + token, id)
        client.expire(prefix + token, ttl)
        return token
    }

    suspend fun deleteToken(token: String) {
        client.del(prefix + token)
    }
}