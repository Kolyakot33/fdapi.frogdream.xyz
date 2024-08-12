package ru.l0sty.plugins

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import ru.l0sty.databases.Card
import ru.l0sty.databases.CardColor
import ru.l0sty.databases.Player
import ru.l0sty.databases.Role
import ru.l0sty.routes.voteRoutes

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json)
    }
}

@Serializable
data class TopEntry(
    val name: String,
    var value: Int
)

object PlayTop {
    private var lastUpdate = 0L
    private var data: List<TopEntry>? = null

    private suspend fun fetchTopStats(url: String): List<TopEntry> {
        return json.decodeFromString(httpClient.get(url).bodyAsText())
    }

    private suspend fun getPlayTop(): List<TopEntry> = coroutineScope {
        val statsFarmsDeferred = async { fetchTopStats("http://localhost:7001/top?stat=PLAY_ONE_MINUTE") }
        val statsBuildsDeferred = async { fetchTopStats("http://localhost:7000/top?stat=PLAY_ONE_MINUTE") }

        val statsFarms = statsFarmsDeferred.await()
        val statsBuilds = statsBuildsDeferred.await()

        val combinedStats = (statsFarms + statsBuilds)
            .groupBy { it.name }
            .map { (name, entries) -> TopEntry(name, entries.sumOf { it.value }) }
            .sortedByDescending { it.value }

        combinedStats
    }

    suspend fun get(): List<TopEntry> {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate > 300000 || data == null) {
            lastUpdate = currentTime
            data = getPlayTop()
        }
        return data!!
    }
}


//suspend fun loadPlayers(): List<PlayerDetailsResponse> {
//    val data = players.find()
//
//    return data.toList().associateWith { PlayTop.get()[it.nickName] }.map { (player, playTime) ->
//       detailedResponse(player, playTime ?: 0)
//    }.sortedByDescending { it.playTime }
//}


fun Application.configureRouting() {
    install(Resources)
    routing {
        voteRoutes()
        get("/") {
            call.respondText("Hello World!")
        }
        get("/top") {
            call.respond(PlayTop.get())

        }
        route("/players") {
            route("/{id}") {
                get {
                    val player = call.parameters["id"]?.let {
                        players.findOne(Player::discordID eq it)
                    } ?: call.parameters["id"]?.let {
                        players.findOne(Player::nickName eq it)
                    } ?: return@get run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"));
                    }
                    call.respond(detailedResponse(player))
                }
                get("/cards") {
                    val player = call.parameters["id"]?.let {
                        players.findOne(Player::discordID eq it)
                    } ?: call.parameters["id"]?.let {
                        players.findOne(Player::nickName eq it)
                    } ?: return@get run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"));
                    }
                    val cards =
                        cards.find(Card::player eq player.nickName).toList().map { CardInfo(it._id, it.name, it.color) }
                    call.respond(cards)

                }

            }
        }
        get("/@me") {
            tokenAuth {
                val player = players.findOne(Player::discordID eq call.discordId) ?: return@get run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"))
                }
                call.respond(player)
            }
        }
        rateLimit(RateLimitName("skinset")) {
            put("/skin") {
                tokenAuth {
                    val player = players.findOne(Player::discordID eq call.discordId) ?: return@put run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"))
                    }
                    val skin = call.receiveText()
                    val resp = httpClient.post("http://node.l0sty.ru:7010/skin/${player.uuid}") {
                        setBody(skin)
                    }
                    call.respond(resp.status)

                }
            }
        }

    }
}

@Serializable
class PlayerDetailsResponse(
    var nickName: String,
    var skin: String,
    var head: String,
    var premium: Boolean,
    var playTime: Int,
    var description: String,
    var roles: List<Role>,
    var isBanned: Boolean
)

@Serializable
data class CardInfo(
    val id: Int,
    val name: String,
    val color: CardColor
)

suspend fun detailedResponse(player: Player): PlayerDetailsResponse {
    val skin = httpClient.get("http://node.l0sty.ru:7010/skin") {
        parameter("nick", player.nickName)
        parameter("uuid", player.uuid)
    }.bodyAsText()
    return PlayerDetailsResponse(
        player.nickName,
        skin,
        "https://new.frogdream.xyz/getUserHead/${player.nickName}.png",
        false,
        PlayTop.get().firstOrNull { it.name == player.nickName }?.value ?: 0,
        "Not implemented",
        player.roles.mapNotNull { roles.findOne(Role::id eq it) },
        player.isBanned
    )
}