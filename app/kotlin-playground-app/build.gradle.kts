import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.jib)
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
    implementation(project(":kotlin-playground-domain"))
    implementation(project(":adapters:kotlin-playground-repositories"))
    implementation(project(":adapters:kotlin-playground-http-services"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.json)
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)

    testImplementation(testFixtures(project(":adapters:kotlin-playground-repositories")))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.tests.jvm)

//    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-swagger:$ktor_version")
//    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")

    kover(project(":kotlin-playground-domain"))
    kover(project(":adapters:kotlin-playground-repositories"))
    kover(project(":adapters:kotlin-playground-http-services"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_22)
            freeCompilerArgs.add("-Xcontext-receivers")
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

jib {
    from {
        image = "openjdk:22"
    }
    container {
        // Hardcoded due to reproducibility https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#please-tell-me-more-about-reproducibility
//        creationTime.set("2023-12-23T14:15:00+01:00")
        mainClass = "com.fabien.app.ApplicationKt"
    }
    to {
        image = "ghcr.io/fabien-r/${project.parent!!.name}:${System.getenv("DOCKER_REF") ?: "local"}"
        auth {
            username = System.getenv("DOCKER_USERNAME")
            password = System.getenv("DOCKER_PASSWORD")
        }
    }
}
