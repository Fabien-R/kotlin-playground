plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqldelight)
    id("java-test-fixtures")
}

group = "com.fabien"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kotlin-playground-domain"))
    implementation(libs.arrow.core)
    implementation(libs.bundles.database)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.jupiter.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testFixturesImplementation(libs.bundles.testcontainers)

    kover(project(":kotlin-playground-domain"))
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

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.fabien")
            dialect(libs.sqldelight.postgresql.get())
        }
    }
}