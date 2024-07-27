package ru.l0sty

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json
import ru.l0sty.plugins.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>): Unit = EngineMain.main(args)

val config = Json {}.decodeFromString<Config>(Files.readString(Path.of("config.json")))


fun Application.module() {
    thread {
        val scn = Scanner(System.`in`)
        while (true) {
            when (scn.nextLine()) {
                "stop" -> {
                    exitProcess(0)
                }
            }
        }
    }
    install(RateLimit) {
        global {
            rateLimiter(limit = 10, refillPeriod = 2.seconds)
        }
        register {
            requestKey { applicationCall ->
                applicationCall.sessions.get<UserSession>()?.discordId ?: "anon"
            }
            rateLimiter(limit = 5, refillPeriod = 60.seconds)
        }
    }

    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureDatabases()
    configureRouting()
    configureBanking()
    configureED()
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Patch)


        allowHeader("Authorization")
        allowHeader("Content-type")
        allowNonSimpleContentTypes = true
    }

}
