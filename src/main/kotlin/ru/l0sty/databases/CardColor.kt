package ru.l0sty.databases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    companion object {
        val Default = Color("cyan")
    }
    val isPremium: Boolean
        get() = this is Image || (this is Color && this.value in Color.premiumColors)

}