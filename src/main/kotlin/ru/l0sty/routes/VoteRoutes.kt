package ru.l0sty.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.litote.kmongo.eq
import ru.l0sty.config
import ru.l0sty.databases.Vote
import ru.l0sty.plugins.discordId
import ru.l0sty.plugins.tokenAuth
import ru.l0sty.plugins.votes

fun Route.voteRoutes() {
    postVote()
}


fun Route.postVote() = post("/vote/{id}") {
    tokenAuth {
        val player = getPlayer() ?: return@post
        if (player.isBanned) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
            return@post
        }
        val id = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_vote_id_required"))
            return@post
        }
        if (id !in config.allowedVotes) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_vote_id_invalid"))
            return@post
        }
        val possibleVotes = votes.find(Vote::playerID eq call.discordId).toList()
        if (possibleVotes.size >= 3 || possibleVotes.any { it.value == id }) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_already_voted"))
            return@post
        }
        votes.insertOne(Vote(player.discordID, Clock.System.now().epochSeconds.toString(), id))
        call.respond(HttpStatusCode.Created, mapOf("remaining_votes" to (3 - possibleVotes.size - 1)))
    }
}
