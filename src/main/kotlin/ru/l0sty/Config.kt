package ru.l0sty

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val easyDonateToken: String,
    val mongoUrl: String,
    val discordToken: String

)