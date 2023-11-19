plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.serialization)
}

group = "com.fabien"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kotlin-playground-domain"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.coroutines.core)
    implementation(libs.mindee.java)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.json)
    implementation(libs.ktor.serialization.xml)

    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.ktor.client.mock)
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