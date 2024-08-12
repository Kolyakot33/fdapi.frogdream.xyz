package ru.l0sty.databases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Role(
    @SerialName("_id")
    val id: String,
    val displayName: String?,
    val prefix: String?,
    val group: String?,
    val discordID: String?,
    val priority: Int?
)
