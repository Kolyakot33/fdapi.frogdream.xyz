package ru.l0sty.routes

import dev.kord.common.entity.Snowflake
import dev.kord.rest.service.RestClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.serialization.Serializable
import org.apache.commons.codec.binary.Hex
import org.litote.kmongo.eq
import org.litote.kmongo.or
import ru.l0sty.config
import ru.l0sty.plugins.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


fun Route.easyDonateRoutes() {
    rateLimit {
        getBuyLink()
    }
    postEDCallback()

}


fun Route.getBuyLink() = get("/buy") {
    val nickName = call.request.queryParameters["nickName"] ?: return@get run {
        call.respondText("NickName is required", status = HttpStatusCode.BadRequest)
    }
    val session = call.sessions.get<UserSession>() ?: return@get run {
        call.respondRedirect("/login?redirectUrl=${call.request.uri}")
    }
    val email = session.email
    val promo = call.request.queryParameters["promo"]

    val possibleUser = players.findOne(
        or(
            Player::nickName eq nickName,
            Player::bedrockNickName eq nickName,
            Player::email eq email,
            Player::uuid eq UUID.nameUUIDFromBytes("OfflinePlayer:$nickName".toByteArray()).toString(),
            Player::discordID eq session.discordId
        )
    )

    if (possibleUser != null) {
        call.respondHtml(HttpStatusCode.Forbidden) {
            body {
                h1 {
                    +"403"
                }
                h2 {
                    +"У тебя уже есть проходка -_-"
                }
            }
        }
        return@get
    }


    val response = httpClient.get {
        url("https://easydonate.ru/api/v3/shop/payment/create")
        headers {
            header("Shop-Key", EDToken)
        }
        parameter("customer", nickName)
        parameter("server_id", 34207)
        parameter("products", "{\"277686\":1}")
        parameter("email", email)
        parameter("success_url", "https://frogdream.xyz/?success=true")
        if (!promo.isNullOrEmpty()) {
            parameter("coupon", promo)
        }
    }.body<EDResponse>()
    response.response.url?.let { url ->
        awaitingPayments.save(
            AwaitingPayment(
                session.discordId,
                nickName,
                email
            )
        )
        call.respondRedirect(url)
    } ?: call.respondHtml(HttpStatusCode.InternalServerError) {
        body {
            h1 {
                +"500"
            }
            h2 {
                +"Не удалось создать платёж :("
            }

        }
    }


}

fun Route.postEDCallback() = post("/easydonate-callback") {
    val body: EasyDonateCallback = call.receive()
    if (!body.checkSignature()) {
        sendWebhook(
            "ПОДПИСЬ callback не совпала!\n" +
                    " $body"
        )
        call.respond(HttpStatusCode.BadRequest)
        return@post
    }
    val nickName = body.customer
    val payment = awaitingPayments.findOne(AwaitingPayment::nickName eq nickName) ?: return@post run {
        sendWebhook("Игрок купил проходку без сайта. $nickName")
    }
    val possibleUser = players.findOne(
        or(
            Player::nickName eq nickName,
            Player::bedrockNickName eq nickName,
            Player::email eq payment.email,
            Player::uuid eq UUID.nameUUIDFromBytes("OfflinePlayer:$nickName".toByteArray()).toString()
        )
    )
    if (possibleUser != null) {
        sendWebhook("Игрок купил проходку, но она у него уже есть\n $body \n $possibleUser")
        call.respond(HttpStatusCode.OK)
        return@post
    }


    players.insertOne(
        Player(
            payment.discordId,
            nickName,
            payment.email
        )
    )
    RestClient(config.discordToken).guild.modifyGuildMember(
        Snowflake("999660962598625300"),
        Snowflake(payment.discordId)
    ) {
        if (roles != null) {
            roles!! += Snowflake("1000753460280565800")
        } else {
            roles = mutableSetOf(Snowflake("1000753460280565800"))
        }
        nickname = nickName
    }
    call.respond(HttpStatusCode.OK)

}


suspend fun sendWebhook(message: String) {
    RestClient(config.discordToken).webhook.executeWebhook(
        Snowflake("1260588890050400306"),
        "CfU33O5neltZFyGIOxjNT4rpuJc5zv3JrYLZBpM1M5e6D7F4H4FKbzNtZ6L4XBSYqQsg"
    ) {
        content = message
    }
}

@Serializable
data class EasyDonateCallback(
    val payment_id: Int,
    val cost: Double,
    val customer: String,
    val signature: String
) {
    fun checkSignature(): Boolean {
        val cost_value = if (cost == cost.toInt().toDouble()) cost.toInt().toString() else cost.toString()
        val payload = "$payment_id@$cost_value@$customer"
        val hMacSHA256 = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(EDToken.toByteArray(), "HmacSHA256")
        hMacSHA256.init(secretKey)
        return Hex.encodeHexString(hMacSHA256.doFinal(payload.toByteArray())) == signature


    }
}


@Serializable
class EDResponse(val response: Result, val success: Boolean) {
    @Serializable
    class Result(val url: String?)
}