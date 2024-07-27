package ru.l0sty.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import ru.l0sty.routes.bankRoutes

fun Application.configureBanking() {
    routing {
        bankRoutes()
    }

}

