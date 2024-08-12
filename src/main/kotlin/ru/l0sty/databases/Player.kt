package ru.l0sty.databases

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

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
    var ownMinecraft: Boolean = false,
    var roles: MutableList<String> = mutableListOf(),
    @Contextual
    val _id: Id<Player> = newId(),
)