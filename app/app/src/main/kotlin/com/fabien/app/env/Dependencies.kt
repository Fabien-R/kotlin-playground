package com.fabien.app.env

import com.fabien.app.authent.JwtService
import com.fabien.app.authent.configureJwt
import com.fabien.app.invoiceExtraction.mindee.mindeeApi
import com.fabien.app.organisationIdentity.insee.InseeApi
import com.fabien.app.organisationIdentity.insee.inseeAuth
import com.fabien.app.organisationIdentity.insee.inseeAuthLoadToken
import com.fabien.app.organisationIdentity.insee.inseeService
import com.fabien.domain.Insee
import com.fabien.domain.Jwt
import com.fabien.domain.Mindee
import com.fabien.domain.Postgres
import com.fabien.domain.repositories.OrganizationRepository
import com.fabien.domain.services.InvoiceExtractionService
import com.fabien.domain.services.OrganizationIdentityService
import com.fabien.repositories.database
import com.fabien.repositories.hikari
import com.fabien.repositories.organizationRepository
import com.mindee.MindeeClient
import io.ktor.client.engine.cio.*

class Dependencies(
    val organizationIdentityService: OrganizationIdentityService,
    val jwtService: JwtService,
    val invoiceExtractionService: InvoiceExtractionService,
    val organizationRepository: OrganizationRepository?,
)

fun dependencies(inseeParams: Insee, jwtParams: Jwt, mindeeParams: Mindee, postgres: Postgres): Dependencies {
    val inseeHttpEngine = CIO.create {
        requestTimeout = 3000
        maxConnectionsCount = 20
        endpoint {
            maxConnectionsPerRoute = 4
            keepAliveTime = 5000
            connectTimeout = 4000
            connectAttempts = 1
        }
    }

    val inseeAuthLoadToken = inseeAuthLoadToken(
        inseeParams.baseApi,
        inseeParams.authenticationApi,
        inseeParams.base64ConsumerKeySecret,
        inseeParams.tokenValiditySeconds,
    )

    val inseeAuthProvider = inseeAuth(inseeAuthLoadToken)

    val inseeService = inseeService(
        InseeApi(
            inseeHttpEngine,
            inseeAuthProvider,
            inseeParams.baseApi,
            inseeParams.siretApi,
        ),
    )

    val mindeeClient = MindeeClient(mindeeParams.apiKey)

    return Dependencies(
        inseeService,
        configureJwt(jwtParams.audience, jwtParams.domain),
        mindeeApi(mindeeClient),
        // TODO should not trigger database connexion for module not requiring DB
        if (postgres.enabled) organizationRepository(database(hikari(postgres)).organizationsQueries) else null,
    )
}
