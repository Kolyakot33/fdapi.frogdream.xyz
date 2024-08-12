package ru.l0sty.databases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AwaitingPayment(
    @SerialName("_id")
    val discordId: String,
    val nickName: String,
    val email: String,
)