plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

group = "com.fabien"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)

    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
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
        finalizedBy(findByName("koverXmlReport")!!.path)
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
        ktlint(libs.versions.klint.get()).editorConfigOverride(
            mapOf(
                "max_line_length" to "160",
            ),
        )
    }
}
