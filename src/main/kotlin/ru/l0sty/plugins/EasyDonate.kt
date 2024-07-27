package ru.l0sty.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import ru.l0sty.config
import ru.l0sty.routes.easyDonateRoutes

val EDToken = config.easyDonateToken
fun Application.configureED() {
    routing {
        easyDonateRoutes()
    }
}

