package com.fabien.app

import com.fabien.app.env.Dependencies
import com.fabien.app.env.dependencies
import com.fabien.app.env.loadConfiguration
import com.fabien.app.invoiceExtraction.configureExtractionRouting
import com.fabien.app.organisationIdentity.configureOrganizationIdentityRouting
import com.fabien.app.organization.configureOrganizationRouting
import com.fabien.app.organization.handler.addOrganizationCommandHandler
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun main() {
    // Since ktor configuration process mix server configuration with business configuration
    // I technically load twice the application.yaml-> manually and inside the commandLineEnvironment
    // /!\ Using EngineMain does not allow loading module with params...
    ApplicationConfig("application.yaml").let { applicationConfig ->
        loadConfiguration(applicationConfig).also { env ->
            val dependencies = dependencies(env.insee, env.jwt, env.mindee, env.postgres)
            val applicationEngineEnvironment = commandLineEnvironment(emptyArray()) {
                module { module(dependencies) }
            }
            val engine = embeddedServer(CIO, applicationEngineEnvironment)
            engine.start(true)
        }
    }
}

fun Application.module(dependencies: Dependencies) {
    install(DefaultHeaders) {
        // add Standard Server & Date headers
    }
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    authentication {
        with(dependencies.jwtService) {
            configure()
        }
    }

    configureOrganizationIdentityRouting(dependencies.organizationIdentityService)

    configureExtractionRouting(dependencies.invoiceExtractionApi)

    dependencies.organizationRepository?.let {
        configureOrganizationRouting(addOrganizationCommandHandler(it))
    }
}
