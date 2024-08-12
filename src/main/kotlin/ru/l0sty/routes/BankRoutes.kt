package ru.l0sty.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.inc
import org.litote.kmongo.setValue
import ru.l0sty.databases.Card
import ru.l0sty.databases.CardColor
import ru.l0sty.databases.Player
import ru.l0sty.plugins.*


const val MAX_CARD_BALANCE = 64 * 9 * 6

fun Route.bankRoutes() = route("/bank") {
    getUserCards()
    getCard()
    patchCard()
    postTransfer()
}


/*
*
*/
suspend fun PipelineContext<Unit, ApplicationCall>.getPlayer(): Player? {
    val player = players.findOne(Player::discordID eq call.discordId) ?: run {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
        return null
    }
    return player
}

suspend fun PipelineContext<Unit, ApplicationCall>.getOwnedCard(id: String?): Card? {
    val player = getPlayer() ?: return null
    val card = id?.toIntOrNull()?.let { cardId ->
        cards.findOne(Card::_id eq cardId)
    } ?: run {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_card_id_required"))
        return null
    }
    if (card.player != player.nickName) {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
        return null
    }
    return card
}


fun Route.getUserCards() = get("/cards") {
    tokenAuth {
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@get run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
        }
        val cards = cards.find(Card::player eq player.nickName).toList()
        call.respond(cards)
    }
}

fun Route.getCard() = get("/cards/{id}") {
    tokenAuth {
        val player = players.findOne(Player::discordID eq call.discordId) ?: return@get run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
        }
        val card = cards.findOne(Card::_id eq call.parameters["id"]?.toInt())
        if (card?.player != player.nickName) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "err_access_denied"))
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
        val card = getOwnedCard(call.parameters["id"]) ?: return@patch
        patchData.name?.let { name ->
            if (name.length > 32) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_name_too_long"))
                return@patch
            }
            cards.updateOneById(card._id, setValue(Card::name, name))
        }
        patchData.color?.let { color ->
            if (color.isPremium && false) { // TODO: Implement premium check
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_premium_required"))
                return@patch
            }
            cards.updateOneById(card._id, setValue(Card::color, color))
        }
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.postTransfer() = post("/cards/{id}/transfer") {
    val parameters = call.receiveParameters()
    val target = parameters["target"]?.toIntOrNull() ?: return@post run {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_target_required"))
    }
    val amount = parameters["amount"]?.toIntOrNull() ?: return@post run {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_amount_required"))
    }
    tokenAuth {
        val card = getOwnedCard(call.parameters["id"]) ?: return@post

        if (amount <= 0) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_amount_must_be_positive"))
            return@post
        }
        if (card.balance < amount) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "err_not_enough_money"))
            return@post
        }
        val targetCard = cards.findOneById(target) ?: return@post run {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "err_target_card_not_found"))
        }
        if (card.locked || targetCard.locked) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "err_card_locked")
            )

            return@post
        }
        if (card._id == targetCard._id) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "err_same_card_transfer")
            )
            return@post
        }
        if (targetCard.balance + amount > MAX_CARD_BALANCE) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "err_target_card_full")
            )
            return@post

        }
//        targetCard.balance += amount
//        cards.updateOne(targetCard)
        cards.updateOneById(targetCard._id, inc(Card::balance, amount))
//        card.balance -= amount
//        cards.updateOne(card)
        cards.updateOneById(card._id, inc(Card::balance, -amount))
        call.respond(HttpStatusCode.OK)
    }
}