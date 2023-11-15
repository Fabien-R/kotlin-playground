plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqldelight)
}

repositories {
    mavenCentral()
}

dependencies {

    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)
    implementation(libs.bundles.database)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.testcontainers)
}

tasks.test {
    useJUnitPlatform()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.fabien")
            dialect(libs.sqldelight.postgresql.get())
        }
    }
}