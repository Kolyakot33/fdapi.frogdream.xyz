package ru.l0sty.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import ru.l0sty.plugins.*


fun Route.bankRoutes() = route("/bank") {
    getUserCards()
    getCard()
    patchCard()
    postTransfer()
}

fun Route.getUserCards() = get("/cards") {
    tokenAuth {
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@get run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        }
        val cards = cards.find(Card::player eq player.nickName).toList()
        call.respond(cards)
    }
}

fun Route.getCard() = get("/cards/{id}") {
    tokenAuth {
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@get run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        }
        val card = cards.findOne(Card::_id eq call.parameters["id"]?.toInt())
        if (card?.player != player.nickName) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        } else {
            call.respond(card)
        }
    }
}

@Serializable
data class CardPatchRequest(
    val name: String? = null,
    val color: CardColor? = null
)

fun Route.patchCard() = patch("/cards/{id}") {
    val patchData = call.receive<CardPatchRequest>()
    tokenAuth {
        val card = cards.findOne(Card::_id eq call.parameters["id"]?.toInt()) ?: return@patch run {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Card not found"))
        }
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@patch run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        }
        if (card.player != player.nickName) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        } else {
            patchData.name?.let { name ->
                if (name.length > 32) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is too long"))
                    return@patch
                }
                cards.updateOneById(card._id, setValue(Card::name, name))
            }
            patchData.color?.let { color ->
                cards.updateOneById(card._id, setValue(Card::color, color))
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.postTransfer() = post("/cards/{id}/transfer") {
    val parameters = call.receiveParameters()
    val target = parameters["target"]?.toIntOrNull() ?: return@post run {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Target is required"))
    }
    val amount = parameters["amount"]?.toIntOrNull() ?: return@post run {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Amount is required"))
    }
    tokenAuth {
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@post run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
        }
        val card = cards.findOne(Card::_id eq call.parameters["id"]?.toInt())
        if (card?.player != player.nickName) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            return@post
        }

        if (amount <= 0) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Amount must be positive"))
            return@post
        }
        if (card.balance < amount) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Not enough money"))
            return@post
        }
        val targetCard = cards.findOneById(target) ?: return@post run {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Target card not found"))
        }
        if (card.locked || targetCard.locked) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "One of the cards is locked")
            )

            return@post
        }
        if (card._id == targetCard._id) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "You can't transfer money to the same card")
            )
            return@post
        }
        if (targetCard.balance + amount > 64 * 9 * 6) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Target card is full")
            )
            return@post

        }
        targetCard.balance += amount
        cards.updateOne(targetCard)
        card.balance -= amount
        cards.updateOne(card)
        call.respond(HttpStatusCode.OK)
    }
}

