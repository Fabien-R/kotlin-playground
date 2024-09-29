import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

    kover(project(":kotlin-playground-domain"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_22)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
    test {
        useJUnitPlatform()
        finalizedBy(findByName("koverXmlReport")!!.path)
    }
}