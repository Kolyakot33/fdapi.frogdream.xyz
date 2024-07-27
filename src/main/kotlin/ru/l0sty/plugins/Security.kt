package ru.l0sty.plugins

import dev.kord.common.entity.DiscordUser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable


val clientId = "1001145803227811893"
val clientSecret = "j-wSpVK1ZVOS5ceCduyOd6mV88wNHkYs"
val DiscordIdKey = AttributeKey<String>("DiscordId")

fun Application.configureSecurity() {


    val redirects = mutableMapOf<String, String>()
    val httpClient = HttpClient() {
        install(ContentNegotiation) {
            json(
                json
            )
        }
    }


    install(Authentication) {
        oauth("oauth-discord") {
            urlProvider = { "https://fdapi.frogdream.xyz/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    defaultScopes = listOf("identify", "email", "guilds.join"),
                    onStateCreated = { call, state ->
                        //saves new state with redirect url value
                        call.request.queryParameters["redirectUrl"]?.let {
                            if (it.startsWith("https://fdapi.frogdream.xyz") || it.startsWith("https://frogdream.xyz") || it.startsWith(
                                    "http://81.24.210.35"
                                )
                            ) {
                                redirects[state] = it
                            }
                        }
                    }
                )
            }
            client = httpClient
        }
    }
    install(Sessions) {
        cookie<UserSession>("user_session", RedisSessionStorage("ktor:session")) {
            cookie.extensions["SameSite"] = "lax"
            cookie.domain = "fdapi.frogdream.xyz"

        }
    }

    routing {

        authenticate("oauth-discord") {
            get("/login") {
            }
            get("/callback") {
                val currentPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                println(currentPrincipal)
                // redirects home if the url is not found before authorization
                currentPrincipal?.let { principal ->
                    principal.state?.let { state ->
                        val user = httpClient.get("https://discord.com/api/users/@me") {
                            headers {
                                append("Authorization", "Bearer ${principal.accessToken}")
                            }
                        }.body<DiscordUser>()
                        call.sessions.set(
                            UserSession(
                                user.id.toString(),
                                principal.accessToken,
                                user.email.value ?: ""
                            )
                        )
                        val token = RedisTokenStorage.createToken(user.id.toString())
                        redirects[state]?.let {
                            call.respondRedirect("$it/?access_token=$token")
                            return@get
                        } ?: call.respondRedirect("https://frogdream.xyz/?access_token=$token")


                        return@get
                    }
                }
                call.respondRedirect("/home")
            }
        }
    }
//    routing {
//        authenticate("myauth1") {
//            get("/protected/route/basic") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
//        authenticate("myauth2") {
//            get("/protected/route/form") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
//    }
}

@Serializable
class UserSession(val discordId: String, val accessToken: String, val email: String)


suspend inline fun PipelineContext<Unit, ApplicationCall>.tokenAuth(next: (PipelineContext<Unit, ApplicationCall>).() -> Unit) {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return run {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No token provided"))
        finish()
    }
    val discordId = RedisTokenStorage.getIdByToken(token) ?: return run {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
        finish()
    }
    call.attributes.put(DiscordIdKey, discordId)
    next()
}

suspend inline fun <T> ApplicationCall.tokenAuth(next: (ApplicationCall).() -> T) {
    val token = request.headers["Authorization"]?.removePrefix("Bearer ") ?: run {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "No token provided"))
        return
    }
    val discordId = RedisTokenStorage.getIdByToken(token) ?: run {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
        return
    }
    attributes.put(DiscordIdKey, discordId)
    next()
}

val ApplicationCall.discordId: String
    get() = this.attributes[DiscordIdKey]