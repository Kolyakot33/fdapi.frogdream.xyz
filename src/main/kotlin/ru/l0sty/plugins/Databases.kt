package ru.l0sty.plugins

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import ru.l0sty.config
import java.util.*
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
fun Application.configureDatabases() {
    runBlocking {
        players.estimatedDocumentCount()
        redis.ping()
    }
}


@Serializable
data class Player(
    val discordID: String,
    var nickName: String,
    val email: String,
    var isBanned: Boolean = false,
    var reason: String? = null,
    var bedrockNickName: String? = null,
    var uuid: String = UUID.randomUUID().toString(),
    var lastNameChange: Long = 0,
    @Contextual
    val _id: Id<Player> = newId(),
)

@Serializable
data class AccessToken(
    val token: String,
    val player: String, // discordId
    @Contextual
    val _id: Id<AccessToken> = newId(),
) {

}

@Serializable
sealed class CardColor {
    @Serializable
    @SerialName("Color")
    data class Color(var value: String) : CardColor() {
        companion object {
            internal val allowedColors = listOf("cyan", "emerald", "red", "yellow")
            internal val premiumColors = listOf("black", "magenta", "blue", "green")
        }

        init {
            if (value !in allowedColors && value !in premiumColors) {
                value = "cyan"
            }
        }
    }

    @Serializable
    @SerialName("Image")
    data class Image(val url: String) : CardColor()

    val Default = CardColor.Color("cyan")
    val isPremium: Boolean
        get() = this is Image || (this is Color && this.value in Color.premiumColors)

}

@Serializable
data class Card(
    val _id: Int,
    var balance: Int,
    val player: String,
    var name: String,
    var locked: Boolean = false,
    var expiresAt: Long,
    var color: CardColor = CardColor.Default,
)

@Serializable
data class AwaitingPayment(
    @SerialName("_id")
    val discordId: String,
    val nickName: String,
    val email: String,
)