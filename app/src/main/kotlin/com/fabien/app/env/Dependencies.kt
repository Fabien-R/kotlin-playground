package com.fabien.app.env

import com.fabien.app.authent.JwtService
import com.fabien.app.authent.configureJwt
import com.fabien.app.invoiceExtraction.InvoiceExtractionApi
import com.fabien.app.invoiceExtraction.mindee.mindeeApi
import com.fabien.app.organisationIdentity.OrganizationIdentityService
import com.fabien.app.organisationIdentity.insee.InseeApi
import com.fabien.app.organisationIdentity.insee.inseeAuth
import com.fabien.app.organisationIdentity.insee.inseeAuthLoadToken
import com.fabien.app.organisationIdentity.insee.inseeService
import com.mindee.MindeeClient
import io.ktor.client.engine.cio.*

class Dependencies(
    val organizationIdentityService: OrganizationIdentityService,
    val jwtService: JwtService,
    val invoiceExtractionApi: InvoiceExtractionApi,
)

fun dependencies(inseeParams: Insee, jwtParams: Jwt, mindeeParams: Mindee): Dependencies {
    val inseeHttpEngine = CIO.create {
        threadsCount = 20
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
    )
}