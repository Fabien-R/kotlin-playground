package com.fabien.env

import arrow.fx.coroutines.continuations.ResourceScope
import com.fabien.organisationIdentity.insee.InseeApi
import com.fabien.organisationIdentity.insee.InseeService
import com.fabien.organisationIdentity.insee.inseeAuth
import com.fabien.organisationIdentity.insee.inseeService
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*

class Dependencies(
    val inseeService: InseeService
)

suspend fun ResourceScope.dependencies(environment: ApplicationEnvironment): Dependencies {
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
        environment.config.property("insee.baseApi").getString(),
        environment.config.property("insee.authenticationApi").getString(),
        environment.config.property("insee.base64ConsumerKeySecret").getString(),
        environment.config.property("insee.tokenValiditySeconds").getString(),
    )
    val inseeService = inseeService(
        InseeApi(
            inseeHttpEngine,
            inseeAuthProvider,
            environment.config.property("insee.baseApi").getString(),
            environment.config.property("insee.siretApi").getString()
        )
    )
    return Dependencies(
        inseeService
    )

}