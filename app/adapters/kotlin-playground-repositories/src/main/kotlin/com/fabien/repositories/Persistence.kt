package com.fabien.repositories

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.fabien.Database
import com.fabien.domain.Postgres
import com.fabien.kotlinplaygroundrepositories.schema
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

fun hikari(env: Postgres): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${env.host}:${env.port}/${env.database}"
            username = env.user
            password = env.password
            driverClassName = "org.postgresql.Driver"
        },
    )

fun database(dataSource: DataSource) = Database(dataSource.asJdbcDriver()).apply {
    Database::class.schema.create(dataSource.asJdbcDriver())
}
