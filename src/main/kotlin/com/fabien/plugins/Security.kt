package com.fabien.plugins

import com.fabien.authent.AUTH0
import com.fabien.authent.JwtService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity(jwtService: JwtService) {
    authentication {
        with(jwtService) {
            configure()
        }
    }
    routing {
        authenticate(AUTH0) {
            get("/secured") {
                call.respondText { "Hey you" }
            }
        }
    }
}
