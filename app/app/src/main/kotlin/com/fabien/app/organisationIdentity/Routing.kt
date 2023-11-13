package com.fabien.app.organisationIdentity

import com.fabien.app.organisationIdentity.insee.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureOrganizationIdentityRouting(organizationIdentityService: OrganizationIdentityService) {
    routing {
        get("/organization/extract") {
            val nationalId = call.parameters["nationalId"]
            val searchText = call.parameters["searchText"]
            val zipCode = call.parameters["zipCode"]
            val pageSize = call.parameters["pageSize"]?.toInt() ?: 5
            val page = call.parameters["page"]?.toInt() ?: 0

            organizationIdentityService.fetchIdentities(nationalId, searchText, zipCode, pageSize, page).respond(HttpStatusCode.OK)
        }
    }
}
