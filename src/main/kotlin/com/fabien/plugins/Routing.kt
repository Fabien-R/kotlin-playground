package com.fabien.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
//    install(StatusPages) {
//        exception<Throwable> { call, cause ->
//            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
//        }
//    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
