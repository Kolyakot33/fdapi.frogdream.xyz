package ru.l0sty.plugins

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import ru.l0sty.config
import ru.l0sty.databases.*
import kotlin.concurrent.thread

val client by lazy {
    KMongo.createClient(config.mongoUrl).coroutine.also {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            it.close()
        })
    }
}

val redis = newClient(Endpoint.from("127.0.0.1:6379"))

val db by lazy { client.getDatabase("flamingo") }
val players by lazy { db.getCollection<Player>("players") }
val cards by lazy { db.getCollection<Card>("cards") }
val awaitingPayments by lazy { db.getCollection<AwaitingPayment>("awaiting_payments") }
val votes by lazy { db.getCollection<Vote>("votes") }
val roles by lazy { db.getCollection<Role>("roles") }

fun Application.configureDatabases() {
    runBlocking {
        players.estimatedDocumentCount()
        redis.ping()
    }
}



