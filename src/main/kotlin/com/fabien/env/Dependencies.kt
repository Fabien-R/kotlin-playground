package com.fabien.env

import com.fabien.authent.JwtService
import com.fabien.authent.configureJwt
import com.fabien.organisationIdentity.insee.InseeApi
import com.fabien.organisationIdentity.insee.InseeService
import com.fabien.organisationIdentity.insee.inseeAuth
import com.fabien.organisationIdentity.insee.inseeService
import io.ktor.client.engine.cio.*

class Dependencies(
    val inseeService: InseeService,
    val jwtService: JwtService,
)
fun dependencies(inseeParams: Insee, jwtParams: Jwt): Dependencies {
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

    val inseeAuthProvider = inseeAuth(
        inseeParams.baseApi,
        inseeParams.authenticationApi,
        inseeParams.base64ConsumerKeySecret,
        inseeParams.tokenValiditySeconds.toString(),
    )
    val inseeService = inseeService(
        InseeApi(
            inseeHttpEngine,
            inseeAuthProvider,
            inseeParams.baseApi,
            inseeParams.siretApi,
        ),
    )

    return Dependencies(
        inseeService,
        configureJwt(jwtParams.audience, jwtParams.domain),
    )
}
