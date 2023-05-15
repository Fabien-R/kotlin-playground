plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {

    implementation(libs.arrow.core)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.json)
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)

    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)

//    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-swagger:$ktor_version")
//    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")

//    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "${JavaVersion.VERSION_19}"
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }
    test {
        useJUnitPlatform()
    }
}

spotless {
    kotlin {
        ktlint(libs.versions.klint.get()).editorConfigOverride(
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
    defaults {
        xml {
            onCheck = true
        }
    }
}
