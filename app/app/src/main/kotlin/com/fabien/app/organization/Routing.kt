package com.fabien.app.organization

import com.fabien.domain.handlers.AddOrganizationCommand
import com.fabien.domain.handlers.AddOrganizationCommandHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureOrganizationRouting(addOrganizationCommandHandler: AddOrganizationCommandHandler) {
    routing {
        post("/organization") {
            with(call.receive<AddOrganizationDTO>()) {
                addOrganizationCommandHandler(this.toCommand()).respond(HttpStatusCode.Created)
            }
        }
//        get("/organization/search") {
//            val searchText = call.parameters["searchText"]
//            val zipCode = call.parameters["zipCode"]
//            val pageSize = call.parameters["pageSize"]?.toInt() ?: 5
//            val page = call.parameters["page"]?.toInt() ?: 0
//
//            organizationQueryHandler(OrganizationQuery(searchText, zipCode, pageSize, page)).respond(HttpStatusCode.OK)
//        }
    }
}

@Serializable
data class AddOrganizationDTO(
    val name: String? = null,
    val nationalId: String? = null,
    val zipCode: String? = null,
    val country: String? = null,
    val city: String? = null,
    val address: String? = null,
    val active: Boolean? = null,
)

private fun AddOrganizationDTO.toCommand() = AddOrganizationCommand(name, nationalId, zipCode, country, city, address, active)
