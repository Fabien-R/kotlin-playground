package com.fabien.app.env

import com.fabien.app.authent.JwtService
import com.fabien.app.authent.configureJwt
import com.fabien.domain.Insee
import com.fabien.domain.Jwt
import com.fabien.domain.Mindee
import com.fabien.domain.Postgres
import com.fabien.domain.repositories.OrganizationRepository
import com.fabien.domain.services.InvoiceExtractionService
import com.fabien.domain.services.OrganizationIdentityService
import com.fabien.http.services.insee.*
import com.fabien.http.services.mindee.mindeeApi
import com.fabien.repositories.database
import com.fabien.repositories.hikari
import com.fabien.repositories.organizationRepository

class Dependencies(
    val organizationIdentityService: OrganizationIdentityService,
    val jwtService: JwtService,
    val invoiceExtractionService: InvoiceExtractionService,
    val organizationRepository: OrganizationRepository?,
)

fun dependencies(inseeParams: Insee, jwtParams: Jwt, mindeeParams: Mindee, postgres: Postgres): Dependencies {
    val inseeService = inseeService(
        inseeApi(
            host = inseeParams.baseApi,
            siretAPI = inseeParams.siretApi,
            authenticationAPI = inseeParams.authenticationApi,
            consumerKeySecret = inseeParams.base64ConsumerKeySecret,
            tokenValiditySeconds = inseeParams.tokenValiditySeconds,
        ),
    )

    return Dependencies(
        inseeService,
        configureJwt(jwtParams.audience, jwtParams.domain),
        mindeeApi(mindeeParams.apiKey),
        // TODO should not trigger database connexion for module not requiring DB
        if (postgres.enabled) organizationRepository(database(hikari(postgres)).organizationsQueries) else null,
    )
}
