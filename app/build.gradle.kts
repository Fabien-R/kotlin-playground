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
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.json)
    implementation(libs.ktor.serialization.xml)
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)
    implementation(libs.mindee.java)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.server.tests.jvm)

//    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-swagger:$ktor_version")
//    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "${JavaVersion.VERSION_19}"
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }
    test {
        useJUnitPlatform {
            excludeTags("mindeeApiCost")
        }
        finalizedBy(findByName("koverXmlReport")!!.path)
    }
}

tasks.register<Test>("mindeeTest") {
    useJUnitPlatform {
    }
    finalizedBy(tasks.findByName("koverXmlReport")!!.path)
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
        ktlint(libs.versions.klint.get()).editorConfigOverride(
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
