package com.fabien.repositories

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.fabien.Database
import com.fabien.Database.Companion.Schema
import com.fabien.domain.Postgres
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
    if(Schema.version == 0L) {
        Schema.create(dataSource.asJdbcDriver())
    }
}
