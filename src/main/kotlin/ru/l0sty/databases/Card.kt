package ru.l0sty.databases

import kotlinx.serialization.Serializable

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