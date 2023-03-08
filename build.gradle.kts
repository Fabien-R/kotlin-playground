val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val mockk_version: String by project

plugins {
    kotlin("jvm") version "1.8.10"
    id("io.ktor.plugin") version "2.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
    id("com.diffplug.spotless") version "6.16.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.0-Alpha"
}

group = "com.fabien"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-apache-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cio-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

spotless {
    kotlin {
        ktlint("0.48.2").editorConfigOverride(
            mapOf(
                "standard:max-line-length" to "160",
                "ktlint_standard_no-wildcard-imports" to "disabled",
            ),
        )
        target(
            "*/kotlin/**/*.kt",
            "src/*/kotlin/**/*.kt",
        )
        targetExclude(
            "*/resources/**/*.kt",
            "src/*/resources/**/*.kt",
            "**/build/**",
            "**/.gradle/**",
        )
    }
    kotlinGradle {
        ktlint("0.48.2").editorConfigOverride(
            mapOf(
                "max_line_length" to "160",
            ),
        )
    }
}

koverReport {
    xml {
        onCheck = true
    }
}
