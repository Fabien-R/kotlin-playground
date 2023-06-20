package com.fabien.env

import com.fabien.authent.JwtService
import com.fabien.authent.configureJwt
import com.fabien.invoiceExtraction.InvoiceExtractionApi
import com.fabien.invoiceExtraction.mindee.mindeeApi
import com.fabien.organisationIdentity.insee.InseeApi
import com.fabien.organisationIdentity.insee.InseeService
import com.fabien.organisationIdentity.insee.inseeAuth
import com.fabien.organisationIdentity.insee.inseeAuthLoadToken
import com.fabien.organisationIdentity.insee.inseeService
import com.mindee.MindeeClientInit
import io.ktor.client.engine.cio.*

class Dependencies(
    val inseeService: InseeService,
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

    val mindeeClient = MindeeClientInit.create(mindeeParams.apiKey)

    return Dependencies(
        inseeService,
        configureJwt(jwtParams.audience, jwtParams.domain),
        mindeeApi(mindeeClient),
    )
}
