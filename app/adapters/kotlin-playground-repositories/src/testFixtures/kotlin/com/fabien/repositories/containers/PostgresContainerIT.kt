package com.fabien.repositories.containers

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.fabien.Database
import com.fabien.domain.Postgres
import com.fabien.repositories.hikari
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@Testcontainers
class PostgresContainerIT {
    private val container = PostgreSQLContainer(DockerImageName.parse("postgres:15.4"))
        .withDatabaseName("Database")
        .withUsername("user")
        .withPassword("password")

    val env: Postgres
    val hikari: DataSource

    init {
        container.start()
        env = Postgres(
            port = container.getMappedPort(5432),
            host = container.host,
            database = container.databaseName,
            user = container.username,
            password = container.password,
            enabled = true,
        )
        hikari = hikari(env)
        // create tables
        Database.Schema.create(hikari.asJdbcDriver())
    }
}
