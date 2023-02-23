package com.fabien.organisationIdentity

import com.fabien.organisationIdentity.insee.InseeApi
import com.fabien.organisationIdentity.insee.InseeService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureOrganizationIdentityRouting() {
    val inseeService = InseeService(InseeApi(this.environment))
    routing {
        get("/organization/search") {
            val nationalId = call.parameters["nationalId"]
            val searchText = call.parameters["searchText"]
            val zipCode = call.parameters["zipCode"]
            val pageSize = call.parameters["pageSize"]?.toInt() ?: 5
            val page = call.parameters["page"]?.toInt() ?: 0

            require(!nationalId.isNullOrEmpty() || !searchText.isNullOrEmpty()) { "nationalId or searchText is mandatory" }

            try {
                call.respondNullable(HttpStatusCode.OK, inseeService.fetchInseeSuppliers())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error with organization identity provider")
                throw e
            }
        }
    }
}