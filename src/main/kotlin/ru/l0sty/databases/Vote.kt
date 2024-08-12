package ru.l0sty.databases

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val playerID: String,
    val time: String,
    val value: String
)