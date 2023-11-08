package com.fabien.app.containers

import com.fabien.app.env.Postgres
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class PostgresContainerIT {
    private val container = PostgreSQLContainer(DockerImageName.parse("postgres:15.4"))
        .withDatabaseName("Database")
        .withUsername("user")
        .withPassword("password")

    val env: Postgres

    init {
        container.start()
        env = Postgres(
            port = container.getMappedPort(5432),
            host = container.host,
            database = container.databaseName,
            user = container.username,
            password = container.password,
        )
    }
}
